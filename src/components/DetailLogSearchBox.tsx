// src/components/DetailLogSearchBox.tsx
import { useState } from 'react';
import { Search } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@/components/ui/select';

// 검색 조건 타입
export interface SearchCriteria {
    traceId: string;
    system: string;
    level: string;
    time: string;
}

// onSearch 타입 정의
interface DetailLogSearchBoxProps {
    onSearch: (criteria: SearchCriteria) => void;
}

const DetailLogSearchBox = ({ onSearch }: DetailLogSearchBoxProps) => {
    const [traceId, setTraceId] = useState('');
    const [system, setSystem] = useState('all');
    const [level, setLevel] = useState('all');
    const [time, setTime] = useState('last_6h');

    // 검색 버튼 클릭 핸들러
    const handleSearch = () => {
        onSearch({
            traceId,
            system,
            level,
            time,
        });
    };

    return (
        <div className="flex w-full flex-wrap items-center gap-3 rounded-lg border bg-white p-4 shadow-sm">
            {/* TraceID 검색창 */}
            <div className="relative flex-1 min-w-[250px]">
                <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                <Input
                    type="text"
                    placeholder="TraceID로 검색..."
                    className="pl-10"
                    value={traceId}
                    onChange={(e) => setTraceId(e.target.value)}
                />
            </div>

            {/* BE/FE/INFRA 필터 */}
            <Select value={system} onValueChange={setSystem}>
                <SelectTrigger className="w-auto min-w-[150px]">
                    <SelectValue placeholder="시스템" />
                </SelectTrigger>
                <SelectContent>
                    <SelectItem value="all">시스템 (전체)</SelectItem>
                    <SelectItem value="FE">FRONT</SelectItem>
                    <SelectItem value="BE">BACK</SelectItem>
                    <SelectItem value="INFRA">INFRA</SelectItem>
                </SelectContent>
            </Select>

            {/* INFO/WARN/ERROR 레벨 */}
            <Select value={level} onValueChange={setLevel}>
                <SelectTrigger className="w-auto min-w-[150px]">
                    <SelectValue placeholder="레벨" />
                </SelectTrigger>
                <SelectContent>
                    <SelectItem value="all">레벨 (전체)</SelectItem>
                    <SelectItem value="INFO">INFO</SelectItem>
                    <SelectItem value="WARN">WARN</SelectItem>
                    <SelectItem value="ERROR">ERROR</SelectItem>
                </SelectContent>
            </Select>

            {/* TODO : 시간 필터, 이건 추후 어떻게 할 지  */}
            <Select value={time} onValueChange={setTime}>
                <SelectTrigger className="w-auto min-w-[150px]">
                    <SelectValue placeholder="시간" />
                </SelectTrigger>
                <SelectContent>
                    <SelectItem value="last_1h">최근 1시간</SelectItem>
                    <SelectItem value="last_6h">최근 6시간</SelectItem>
                    <SelectItem value="last_24h">최근 24시간</SelectItem>
                    <SelectItem value="last_7d">최근 7일</SelectItem>
                </SelectContent>
            </Select>

            {/* 검색 버튼 */}
            <Button
                onClick={handleSearch}
                className="bg-blue-600 text-white hover:bg-blue-500"
            >
                검색
            </Button>
        </div>
    );
};

export default DetailLogSearchBox;