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

    -- ====================================================
    -- 🔥 프론트엔드 배열 로그 처리 (재귀 호출)
    -- ====================================================
    if record["logs"] and type(record["logs"]) == "table" then
        local logs_array = record["logs"]
        local results = {}

        -- 🔥 최상위 레코드의 project_uuid를 가져옴 (Fluent Bit에서 주입한 것)
        local parent_project_uuid = record["project_uuid"]
        local parent_service_name = record["service_name"]

        for i, log_entry in ipairs(logs_array) do
            if type(log_entry) == "table" then
                -- 🔥 하위 로그에 project_uuid 전달
                log_entry["project_uuid"] = log_entry["project_uuid"] or parent_project_uuid
                log_entry["service_name"] = log_entry["service_name"] or parent_service_name

                -- 각 로그 항목을 개별적으로 변환 (재귀 호출)
                local code, ts, transformed = transform_log(tag, timestamp, log_entry)
                if code == 1 and transformed then
                    table.insert(results, transformed)
                end
            end
        end

        -- 변환된 로그들을 개별 레코드로 반환
        if #results > 0 then
            return 2, timestamp, results
        else
            return -1, timestamp, record  -- 처리 실패 시 drop
        end
    end

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

    -- timestamp 정규화: timestamp 또는 @timestamp를 @timestamp로 통일
    local ts = record["@timestamp"] or record["timestamp"] or now
    new_record["@timestamp"] = ts

    new_record["indexed_at"] = now
    new_record["service_name"] = record["service_name"] or record["app_name"] or "unknown-service"
    new_record["component_name"] = record["component_name"] or record["logger"] or "unknown"
    new_record["logger"] = record["package"] or record["logger"] or "unknown"

    ----------------------------------------------------
    -- 3️⃣ layer 추출 및 정규화
    ----------------------------------------------------
    local layer = record["layer"]
    local original_layer = layer

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
        elseif layer == "FRONT" or layer == "FRONTEND" then
            layer = "Frontend"
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
    -- 4️⃣ source_type 자동 분류 로직 개선
    ----------------------------------------------------
    local src = record["source_type"]

    if not src then
        if original_layer then
            local upper_original = string.upper(tostring(original_layer))
            if upper_original == "FRONT" or upper_original == "FRONTEND" or upper_original == "FE" then
                src = "FE"
            elseif upper_original == "BACK" or upper_original == "BACKEND" or upper_original == "BE" then
                src = "BE"
            elseif upper_original == "INFRA" then
                src = "INFRA"
            end
        end

        -- src가 아직 결정되지 않았으면 기존 로직 수행
        if not src then
            local hint = ""
            if layer then
                hint = string.upper(tostring(layer))
            elseif tag then
                hint = string.upper(tostring(tag))
            elseif record["logger"] then
                hint = string.upper(tostring(record["logger"]))
            end

            -- FE / FRONTEND 매칭
            if string.match(hint, "FRONTEND") or string.match(hint, "FRONT") or string.match(hint, "^FE$") then
                src = "FE"
                -- BE / BACK / BACKEND 매칭
            elseif string.match(hint, "BACKEND") or string.match(hint, "BACK") or string.match(hint, "^BE$") then
                src = "BE"
                -- INFRA 매칭
            elseif string.match(hint, "INFRA") then
                src = "INFRA"
                -- 나머지는 OTHERS
            else
                src = "OTHERS"
            end
        end
    end

    new_record["source_type"] = src

    ----------------------------------------------------
    -- 5️⃣ log level / trace_id / message
    ----------------------------------------------------
    local lvl = record["level"] or record["log_level"] or "INFO"
    lvl = string.upper(lvl)
    if lvl ~= "INFO" and lvl ~= "WARN" and lvl ~= "ERROR" then
        lvl = "INFO"
    end
    new_record["level"] = lvl
    new_record["log_level"] = lvl

    -- trace_id 정규화: traceId → trace_id
    local trace_id = record["trace_id"] or record["traceId"]
    if not trace_id and record["message"] then
        trace_id = string.match(record["message"], "([a-f0-9%-]{36})")
    end
    new_record["trace_id"] = trace_id or "unknown"

    -- message: JSON 내부의 message 필드 사용
    new_record["message"] = original_message or record["message"] or "parsed JSON log"

    ----------------------------------------------------
    -- 6️⃣ 추가 메타정보
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
    -- 7️⃣ log_details: 상세 정보 수집 (스키마 준수)
    ----------------------------------------------------
    -- execution_time 정규화: executionTimeMs → execution_time_ms
    local exec_time = record["execution_time_ms"]
            or record["executionTimeMs"]
            or record["execution_time"]
            or nil
    if type(exec_time) == "string" then
        exec_time = tonumber(exec_time)
    end
    new_record["duration"] = exec_time

    local log_details = {}
    local has_details = false

    -- execution_time (long)
    if exec_time then
        log_details["execution_time"] = exec_time
        has_details = true
    end

    -- request 정보
    if record["request"] and type(record["request"]) == "table" then
        local req = record["request"]

        -- http_method (keyword)
        log_details["http_method"] = req["method"] or (req["http"] and req["http"]["method"])

        -- request_uri (text)
        log_details["request_uri"] = req["endpoint"] or (req["http"] and req["http"]["endpoint"])

        -- request_headers (flattened) - headers가 있으면
        if req["headers"] and type(req["headers"]) == "table" then
            log_details["request_headers"] = req["headers"]
        end

        if req then
            if req["parameters"] and type(req["parameters"]) == "table" then
                log_details["request_body"] = req["parameters"]
            else
                log_details["request_body"] = req
            end
        end

        has_details = true
    end

    -- response 정보
    if record["response"] and type(record["response"]) == "table" then
        local res = record["response"]

        -- response_status (integer)
        log_details["response_status"] = tonumber(res["statusCode"] or (res["http"] and res["http"]["statusCode"]))

        -- response_body (flattened)
        if res then
            log_details["response_body"] = res
        end

        has_details = true
    end

    -- exception 정보
    if record["exception"] then
        -- exception_type (keyword)
        log_details["exception_type"] = tostring(record["exception"])
        has_details = true
    end

    -- class_name, method_name (keyword)
    if new_record["class_name"] then
        log_details["class_name"] = new_record["class_name"]
        has_details = true
    end
    if new_record["method_name"] then
        log_details["method_name"] = new_record["method_name"]
        has_details = true
    end

    -- additional_info (flattened) - 기타 추가 정보
    local additional_info = {}
    local has_additional = false

    -- 원본 request/response 객체를 additional_info에 보관
    if record["request"] and type(record["request"]) == "table" then
        additional_info["full_request"] = record["request"]
        has_additional = true
    end
    if record["response"] and type(record["response"]) == "table" then
        additional_info["full_response"] = record["response"]
        has_additional = true
    end
    if record["exception"] and type(record["exception"]) == "table" then
        additional_info["full_exception"] = record["exception"]
        has_additional = true
    end

    if has_additional then
        log_details["additional_info"] = additional_info
        has_details = true
    end

    if has_details then
        new_record["log_details"] = log_details
    end

    ----------------------------------------------------
    -- 8️⃣  requester_ip 및 stack_trace 추출
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

    -- stack_trace (최상위 필드)
    local stacktrace = record["stack_trace"] or record["stackTrace"] or record["stacktrace"] or nil
    new_record["stacktrace"] = stacktrace

    -- log_details에도 stacktrace 추가
    if stacktrace and new_record["log_details"] then
        new_record["log_details"]["stacktrace"] = stacktrace
    end

    ----------------------------------------------------
    -- 9️⃣ MySQL 인프라 로그 특화 처리
    ----------------------------------------------------
    if src == "INFRA" and new_record["component_name"] == "MySQL" then
        -- MySQL Error Log 처리
        if string.match(tostring(tag), "mysql%.error") then
            new_record["logger"] = "MySQL-Error"

            -- thread_id 저장
            if record["thread_id"] then
                new_record["thread_name"] = "mysql-thread-" .. record["thread_id"]
            end

            -- error_code 저장
            if record["error_code"] then
                if not new_record["log_details"] then
                    new_record["log_details"] = {}
                end
                new_record["log_details"]["error_code"] = record["error_code"]
            end

            -- subsystem 정보
            if record["subsystem"] then
                new_record["component_name"] = "MySQL-" .. record["subsystem"]
            end

            -- level 매핑 (Warning -> WARN, System -> INFO, Error -> ERROR)
            if record["level"] then
                local mysql_level = record["level"]
                if mysql_level == "Warning" then
                    new_record["level"] = "WARN"
                    new_record["log_level"] = "WARN"
                elseif mysql_level == "System" then
                    new_record["level"] = "INFO"
                    new_record["log_level"] = "INFO"
                elseif mysql_level == "Error" then
                    new_record["level"] = "ERROR"
                    new_record["log_level"] = "ERROR"
                end
            end

            -- MySQL Slow Query Log 처리
        elseif string.match(tostring(tag), "mysql%.slow") then
            new_record["logger"] = "MySQL-SlowQuery"

            new_record["requester_ip"] = nil

            -- Query 성능 정보
            local query_time = record["query_time"]
            local lock_time = record["lock_time"]
            local rows_sent = record["rows_sent"]
            local rows_examined = record["rows_examined"]

            if query_time then
                new_record["duration"] = tonumber(query_time) * 1000  -- 초를 밀리초로
            end

            if not new_record["log_details"] then
                new_record["log_details"] = {}
            end

            if query_time then
                new_record["log_details"]["query_time"] = tonumber(query_time)
            end
            if lock_time then
                new_record["log_details"]["lock_time"] = tonumber(lock_time)
            end
            if rows_sent then
                new_record["log_details"]["rows_sent"] = tonumber(rows_sent)
            end
            if rows_examined then
                new_record["log_details"]["rows_examined"] = tonumber(rows_examined)
            end

            -- 데이터베이스 이름
            if record["database"] then
                new_record["log_details"]["database"] = record["database"]
            end

            -- SQL 쿼리를 message에 저장
            if record["query"] and record["query"] ~= "" then
                -- 앞뒤 공백 제거
                local clean_query = string.gsub(record["query"], "^%s+", "")
                clean_query = string.gsub(clean_query, "%s+$", "")

                if clean_query ~= "" and not string.match(clean_query, "^SET timestamp=") and not string.match(clean_query, "^use ") then
                    new_record["message"] = clean_query
                end
            end
        end
    end

    return 1, timestamp, new_record
end
