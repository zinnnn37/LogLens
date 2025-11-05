-- ===========================================
-- transform_log (Fluent Bit safe JSON parser)
-- ===========================================

-- 간단한 JSON 파서: key:value를 테이블로 추출
local function simple_json_parse(str)
    if not str or type(str) ~= "string" then
        return nil
    end
    local obj = {}
    for key, val in string.gmatch(str, '"([^"]+)"%s*:%s*"([^"]-)"') do
        obj[key] = val
    end
    for key, val in string.gmatch(str, '"([^"]+)"%s*:%s*([%d%.%-]+)') do
        if tonumber(val) then
            obj[key] = tonumber(val)
        end
    end
    if next(obj) then
        return obj
    else
        return nil
    end
end

function transform_log(tag, timestamp, record)
    local new_record = {}
    local now = os.date("!%Y-%m-%dT%H:%M:%S.000Z")

    ----------------------------------------------------
    -- 1️⃣ message가 JSON 문자열이라면 내부 필드 병합
    ----------------------------------------------------
    local original_message = nil
    if record["message"] and type(record["message"]) == "string" then
        local msg = record["message"]
        if string.sub(msg, 1, 1) == "{" then
            local parsed = simple_json_parse(msg)
            if parsed then
                -- JSON 내부의 message 필드를 따로 저장
                original_message = parsed["message"]
                -- message 안의 필드들을 record로 병합
                for k, v in pairs(parsed) do
                    record[k] = record[k] or v
                end
            end
        end
    end

    ----------------------------------------------------
    -- 2️⃣ 필드 매핑 및 기본값 설정
    ----------------------------------------------------
    new_record["project_uuid"] = record["project_uuid"] or "default-project"
    new_record["timestamp"] = record["@timestamp"] or record["timestamp"] or now
    new_record["indexed_at"] = now
    new_record["service_name"] = record["service_name"] or record["app_name"] or "unknown-service"
    new_record["logger"] = record["package"] or record["logger"] or "unknown"
    new_record["source_type"] = record["source_type"] or "BE"

    ----------------------------------------------------
    -- 3️⃣ layer 추출 및 정규화
    ----------------------------------------------------
    local layer = record["layer"]

    -- layer 대소문자 정규화 (CONTROLLER → Controller)
    if layer then
        if layer == "CONTROLLER" then
            layer = "Controller"
        elseif layer == "SERVICE" then
            layer = "Service"
        elseif layer == "REPOSITORY" then
            layer = "Repository"
        elseif layer == "FILTER" then
            layer = "Filter"
        elseif layer == "UNKNOWN" then
            layer = "Other"
        end
    end

    -- layer가 없으면 logger에서 자동 추출
    if not layer and new_record["logger"] then
        local log = new_record["logger"]
        if string.match(log, "Controller") then
            layer = "Controller"
        elseif string.match(log, "Service") then
            layer = "Service"
        elseif string.match(log, "Repository") then
            layer = "Repository"
        elseif string.match(log, "Filter") then
            layer = "Filter"
        else
            layer = "Other"
        end
    end
    new_record["layer"] = layer or "Other"

    ----------------------------------------------------
    -- 4️⃣ log level / trace_id / message
    ----------------------------------------------------
    local lvl = record["level"] or record["log_level"] or "INFO"
    lvl = string.upper(lvl)
    if lvl ~= "INFO" and lvl ~= "WARN" and lvl ~= "ERROR" then
        lvl = "INFO"
    end
    new_record["level"] = lvl
    new_record["log_level"] = lvl

    local trace_id = record["trace_id"]
    if not trace_id and record["message"] then
        trace_id = string.match(record["message"], "([a-f0-9%-]{36})")
    end
    new_record["trace_id"] = trace_id or "unknown"

    -- message: JSON 내부의 message 필드 사용
    new_record["message"] = original_message or record["message"] or "parsed JSON log"

    ----------------------------------------------------
    -- 5️⃣ 추가 메타정보
    ----------------------------------------------------
    local comment_parts = {}
    if record["thread"] then
        table.insert(comment_parts, "thread: " .. record["thread"])
    end
    if record["app_name"] then
        table.insert(comment_parts, "app: " .. record["app_name"])
    end
    if record["pid"] then
        table.insert(comment_parts, "pid: " .. tostring(record["pid"]))
    end
    new_record["comment"] = table.concat(comment_parts, ", ")

    new_record["class_name"] = record["package"] or record["class_name"] or nil
    -- method_name: message에서 추출 (예: "Request received: createUser" → "createUser")
    local method_name = record["method_name"]
    if not method_name and original_message then
        -- "Request received: XXX" 또는 "Response completed: XXX" 패턴에서 추출
        method_name = string.match(original_message, "Request received: ([%w_]+)")
        if not method_name then
            method_name = string.match(original_message, "Response completed: ([%w_]+)")
        end
    end
    -- request.method에서도 시도
    if not method_name and record["request"] and type(record["request"]) == "table" then
        method_name = record["request"]["method"]
    end
    new_record["method_name"] = method_name or nil
    new_record["thread_name"] = record["thread_name"] or record["thread"] or nil

    ----------------------------------------------------
    -- 6️⃣ execution_time / request / response / exception
    ----------------------------------------------------
    local exec_time = record["execution_time_ms"] or record["execution_time"] or nil
    if type(exec_time) == "string" then
        exec_time = tonumber(exec_time)
    end
    new_record["duration"] = exec_time

    local log_details = {}
    local has_details = false

    if exec_time then
        log_details["execution_time"] = exec_time
        has_details = true
    end

    if record["request"] and type(record["request"]) == "table" then
        local req = record["request"]
        log_details["http_method"] = req["method"] or (req["http"] and req["http"]["method"])
        log_details["request_uri"] = req["endpoint"] or (req["http"] and req["http"]["endpoint"])
        has_details = true
    end

    if record["response"] and type(record["response"]) == "table" then
        local res = record["response"]
        log_details["response_status"] = tonumber(res["statusCode"] or (res["http"] and res["http"]["statusCode"]))
        log_details["response_body"] = res["result"] or nil
        has_details = true
    end

    if record["exception"] then
        log_details["exception_type"] = tostring(record["exception"])
        has_details = true
    end

    if new_record["class_name"] then
        log_details["class_name"] = new_record["class_name"]
        has_details = true
    end
    if new_record["method_name"] then
        log_details["method_name"] = new_record["method_name"]
        has_details = true
    end
    if has_details then
        new_record["log_details"] = log_details
    end

    ----------------------------------------------------
    -- 7️⃣ requester_ip 추출 (여러 소스에서 시도)
    ----------------------------------------------------
    local requester_ip = record["requester_ip"]
            or record["client_ip"]
            or record["remote_addr"]
            or record["x_forwarded_for"]

    -- request 객체 내부에서도 시도
    if not requester_ip and record["request"] and type(record["request"]) == "table" then
        local req = record["request"]
        requester_ip = req["ip"] or req["client_ip"] or req["remote_addr"]

        -- http 하위 객체도 체크
        if not requester_ip and req["http"] and type(req["http"]) == "table" then
            requester_ip = req["http"]["ip"] or req["http"]["client_ip"]
        end
    end

    new_record["requester_ip"] = requester_ip or nil
    new_record["stack_trace"] = record["stack_trace"] or nil

    return 1, timestamp, new_record
end
