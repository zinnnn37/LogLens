-- ===========================================
-- detect_source_type: 파일 경로 기반으로 BE/FE/INFRA 구분
-- ===========================================
function detect_source_type(tag, timestamp, record)
    local source_path = record["file"] or ""
    if string.find(source_path, "/logs/be/") then
        record["source_type"] = "BE"
    elseif string.find(source_path, "/logs/fe/") then
        record["source_type"] = "FE"
    elseif string.find(source_path, "/logs/infra/") then
        record["source_type"] = "INFRA"
    else
        record["source_type"] = "BE"  -- 기본값 BE
    end
    return 1, timestamp, record
end


-- ===========================================
-- transform_log: Spring Boot 로그를 OpenSearch 스키마에 맞게 변환
-- ===========================================
function transform_log(tag, timestamp, record)
    local new_record = {}

    -- record가 이미 파싱된 JSON 객체라고 가정
    -- Fluent Bit의 JSON parser가 자동으로 처리함
    local source = record

    -- ===================================
    -- 1. project_uuid (필수)
    -- ===================================
    new_record["project_uuid"] = record["project_uuid"] or record["PROJECT_UUID"] or "default-project"

    -- ===================================
    -- 2. timestamp (필수)
    -- ===================================
    if record["@timestamp"] then
        new_record["timestamp"] = record["@timestamp"]
    elseif record["timestamp"] then
        new_record["timestamp"] = record["timestamp"]
    else
        new_record["timestamp"] = os.date("!%Y-%m-%dT%H:%M:%S.000Z", timestamp)
    end

    -- ===================================
    -- 3. service_name (필수)
    -- ===================================
    new_record["service_name"] = record["service_name"] or record["app_name"] or "unknown-service"

    -- ===================================
    -- 4. logger (선택) - package 필드 우선
    -- ===================================
    if record["package"] and record["package"] ~= "" then
        new_record["logger"] = record["package"]
    elseif record["logger"] and record["logger"] ~= "" then
        new_record["logger"] = record["logger"]
    else
        new_record["logger"] = nil
    end

    -- ===================================
    -- 5. source_type (선택: FE, BE, INFRA)
    -- ===================================
    local source_type = record["source_type"] or "BE"
    if source_type == "FE" or source_type == "BE" or source_type == "INFRA" then
        new_record["source_type"] = source_type
    else
        new_record["source_type"] = "BE"
    end

    -- ===================================
    -- 6. layer (선택: Controller, Service, Repository, Filter, Util, Other)
    -- ===================================
    if record["layer"] then
        local layer = record["layer"]
        -- UPPERCASE를 Title Case로 변환 (SERVICE -> Service)
        if layer == "CONTROLLER" then
            new_record["layer"] = "Controller"
        elseif layer == "SERVICE" then
            new_record["layer"] = "Service"
        elseif layer == "REPOSITORY" then
            new_record["layer"] = "Repository"
        elseif layer == "FILTER" then
            new_record["layer"] = "Filter"
        elseif layer == "UTIL" then
            new_record["layer"] = "Util"
        else
            new_record["layer"] = layer
        end
    else
        -- logger에서 자동 추출
        local logger = new_record["logger"] or ""
        if string.match(logger, "[Cc]ontroller") then
            new_record["layer"] = "Controller"
        elseif string.match(logger, "[Ss]ervice") then
            new_record["layer"] = "Service"
        elseif string.match(logger, "[Rr]epository") then
            new_record["layer"] = "Repository"
        elseif string.match(logger, "[Ff]ilter") then
            new_record["layer"] = "Filter"
        elseif string.match(logger, "[Uu]til") then
            new_record["layer"] = "Util"
        else
            new_record["layer"] = nil
        end
    end

    -- ===================================
    -- 7. log_level & level (필수: INFO, WARN, ERROR)
    -- ===================================
    local level = record["level"] or record["log_level"] or "INFO"
    level = string.upper(level)
    if level == "DEBUG" or level == "TRACE" then
        level = "INFO"
    elseif level ~= "ERROR" and level ~= "WARN" and level ~= "INFO" then
        level = "INFO"
    end
    new_record["log_level"] = level
    new_record["level"] = level  -- AI 코드 호환성

    -- ===================================
    -- 8. message (필수) - 실제 메시지만 추출
    -- ===================================
    local message = record["message"] or record["log"] or ""

    -- message가 JSON 문자열인 경우 파싱하여 실제 message 추출
    if string.sub(message, 1, 1) == "{" then
        -- JSON 문자열에서 message 필드만 추출 시도
        local success, parsed_msg = pcall(function()
            local msg_pattern = '"message"%s*:%s*"([^"]*)"'
            return string.match(message, msg_pattern)
        end)
        if success and parsed_msg and parsed_msg ~= "" then
            message = parsed_msg
        end
    end

    new_record["message"] = message

    -- ===================================
    -- 9. comment (선택)
    -- ===================================
    local comment_parts = {}
    local thread = record["thread"] or record["thread_name"]
    if thread then
        table.insert(comment_parts, "thread: " .. thread)
    end
    local app = record["app_name"]
    if app then
        table.insert(comment_parts, "app: " .. app)
    end
    local pid = record["pid"]
    if pid then
        table.insert(comment_parts, "pid: " .. tostring(pid))
    end
    if #comment_parts > 0 then
        new_record["comment"] = table.concat(comment_parts, ", ")
    end

    -- ===================================
    -- 10. method_name (선택)
    -- ===================================
    new_record["method_name"] = record["method_name"] or record["method"]

    -- ===================================
    -- 11. class_name (선택)
    -- ===================================
    new_record["class_name"] = record["class_name"] or record["class"]

    -- ===================================
    -- 12. thread_name (선택)
    -- ===================================
    new_record["thread_name"] = record["thread_name"] or record["thread"]

    -- ===================================
    -- 13. trace_id (선택, 기본값: unknown)
    -- ===================================
    local trace_id = nil

    -- 1순위: record의 trace_id 필드 확인
    if record["trace_id"] and record["trace_id"] ~= "" and record["trace_id"] ~= "null" then
        trace_id = record["trace_id"]
    end

    -- 2순위: message가 JSON 문자열인 경우 파싱하여 trace_id 추출
    if not trace_id then
        local msg = record["message"] or ""
        -- JSON 문자열 감지 및 파싱 시도
        if string.sub(msg, 1, 1) == "{" then
            local success, parsed = pcall(function()
                -- Lua에서 간단한 JSON 파싱 (trace_id만 추출)
                local trace_pattern = '"trace_id"%s*:%s*"([^"]+)"'
                return string.match(msg, trace_pattern)
            end)
            if success and parsed then
                trace_id = parsed
            end
        end
    end

    -- 3순위: message에서 UUID 패턴 직접 추출
    if not trace_id then
        local msg = new_record["message"] or ""
        local trace_pattern = "([a-f0-9]{8}%-[a-f0-9]{4}%-[a-f0-9]{4}%-[a-f0-9]{4}%-[a-f0-9]{12})"
        trace_id = string.match(msg, trace_pattern)
    end

    -- 4순위: 기본값
    new_record["trace_id"] = trace_id or "unknown"

    -- ===================================
    -- 14. requester_ip (선택)
    -- ===================================
    if record["requester_ip"] and record["requester_ip"] ~= "" then
        new_record["requester_ip"] = record["requester_ip"]
    elseif record["remote_addr"] then
        new_record["requester_ip"] = record["remote_addr"]
    elseif record["client_ip"] then
        new_record["requester_ip"] = record["client_ip"]
    end

    -- ===================================
    -- 15. duration (선택, 단위: ms)
    -- ===================================
    if record["duration"] then
        new_record["duration"] = tonumber(record["duration"])
    elseif record["execution_time_ms"] then
        new_record["duration"] = tonumber(record["execution_time_ms"])
    elseif record["execution_time"] then
        new_record["duration"] = tonumber(record["execution_time"])
    end

    -- ===================================
    -- 16. stack_trace (선택)
    -- ===================================
    if record["stack_trace"] then
        new_record["stack_trace"] = record["stack_trace"]
    elseif record["exception"] and type(record["exception"]) == "table" and record["exception"]["stack_trace"] then
        new_record["stack_trace"] = record["exception"]["stack_trace"]
    end

    -- ===================================
    -- 17. log_details (선택, nested object)
    -- ===================================
    local log_details = {}
    local has_details = false

    -- 예외 정보
    if record["exception"] and type(record["exception"]) == "table" then
        if record["exception"]["type"] then
            log_details["exception_type"] = record["exception"]["type"]
            has_details = true
        end
    end

    -- 실행 시간
    if record["execution_time_ms"] or record["execution_time"] then
        log_details["execution_time"] = tonumber(record["execution_time_ms"] or record["execution_time"])
        has_details = true
    end

    -- HTTP 요청 정보 (request 객체에서 추출)
    if record["request"] and type(record["request"]) == "table" then
        if record["request"]["http"] and type(record["request"]["http"]) == "table" then
            if record["request"]["http"]["method"] then
                log_details["http_method"] = record["request"]["http"]["method"]
                has_details = true
            end
            if record["request"]["http"]["endpoint"] or record["request"]["http"]["uri"] then
                log_details["request_uri"] = record["request"]["http"]["endpoint"] or record["request"]["http"]["uri"]
                has_details = true
            end
        end
        if record["request"]["headers"] then
            log_details["request_headers"] = record["request"]["headers"]
            has_details = true
        end
        if record["request"]["body"] then
            log_details["request_body"] = tostring(record["request"]["body"])
            has_details = true
        end
    end

    -- HTTP 응답 정보 (response 객체에서 추출)
    if record["response"] and type(record["response"]) == "table" then
        if record["response"]["http"] and type(record["response"]["http"]) == "table" then
            if record["response"]["http"]["statusCode"] or record["response"]["http"]["status"] then
                log_details["response_status"] = tonumber(record["response"]["http"]["statusCode"] or record["response"]["http"]["status"])
                has_details = true
            end
            if record["response"]["http"]["method"] then
                log_details["http_method"] = log_details["http_method"] or record["response"]["http"]["method"]
                has_details = true
            end
            if record["response"]["http"]["endpoint"] then
                log_details["request_uri"] = log_details["request_uri"] or record["response"]["http"]["endpoint"]
                has_details = true
            end
        end
        if record["response"]["result"] then
            -- result가 테이블이면 직접 저장, 아니면 문자열로 변환
            if type(record["response"]["result"]) == "table" then
                log_details["response_body"] = record["response"]["result"]
            else
                log_details["response_body"] = tostring(record["response"]["result"])
            end
            has_details = true
        end
        if record["response"]["method"] then
            log_details["method_name"] = record["response"]["method"]
            has_details = true
        end
    end

    if new_record["class_name"] then
        log_details["class_name"] = new_record["class_name"]
        has_details = true
    end

    if new_record["method_name"] then
        log_details["method_name"] = log_details["method_name"] or new_record["method_name"]
        has_details = true
    end

    if has_details then
        new_record["log_details"] = log_details
    end

    -- ===================================
    -- 18. indexed_at (현재 시각)
    -- ===================================
    new_record["indexed_at"] = os.date("!%Y-%m-%dT%H:%M:%S.000Z")

    return 1, timestamp, new_record
end
