// src/components/DetailLogSearchTable.tsx
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@/components/ui/table';
import { Badge } from '@/components/ui/badge';
import type { LogRow } from '@/components/LogResultsTable';
import { cn } from '@/lib/utils';

const LogLevelIndicator = ({ level }: { level: LogRow['level'] }) => {
    return (
        <span
            className={cn(
                'mr-2 inline-block h-2.5 w-2.5 rounded-full',
                level === 'INFO' && 'bg-blue-500',
                level === 'WARN' && 'bg-yellow-500',
                level === 'ERROR' && 'bg-red-500',
            )}
        />
    );
};

const formatTimestamp = (isoString: string) => {
    try {
        const date = new Date(isoString);
        const mm = String(date.getMonth() + 1).padStart(2, '0');
        const dd = String(date.getDate()).padStart(2, '0');
        const hh = String(date.getHours()).padStart(2, '0');
        const min = String(date.getMinutes()).padStart(2, '0');
        const ss = String(date.getSeconds()).padStart(2, '0');
        return `${mm}-${dd} ${hh}:${min}:${ss}`;
    } catch (_error) {
        return 'Invalid Date';
    }
};

interface DetailLogSearchTableProps {
    logs: LogRow[];
}

const DetailLogSearchTable = ({ logs }: DetailLogSearchTableProps) => {
    return (
        <div className="rounded-lg border bg-white shadow-sm">
            <Table>
                {/* 테이블 헤더 */}
                <TableHeader>
                    <TableRow className="hover:bg-transparent">
                        <TableHead className="w-[100px]">Level</TableHead>
                        <TableHead className="w-[120px]">System</TableHead>
                        <TableHead>Message</TableHead>
                        <TableHead className="w-[180px] text-right">Date</TableHead>
                    </TableRow>
                </TableHeader>

                {/* 테이블 본문 */}
                <TableBody>
                    {logs.length === 0 ? (
                        <TableRow>
                            <TableCell colSpan={4} className="h-24 text-center">
                                검색 결과가 없습니다.
                            </TableCell>
                        </TableRow>
                    ) : (
                        logs.map((log) => (
                            <TableRow key={log.id}>
                                {/* Level */}
                                <TableCell>
                                    <div className="flex items-center">
                                        <LogLevelIndicator level={log.level} />
                                        {log.level}
                                    </div>
                                </TableCell>
                                {/* System */}
                                <TableCell>
                                    <Badge
                                        variant="outline"
                                        className={cn(
                                            'font-mono',
                                            log.layer === 'FE' && 'border-sky-300 bg-sky-50',
                                            log.layer === 'BE' && 'border-indigo-300 bg-indigo-50',
                                            log.layer === 'INFRA' && 'border-gray-400 bg-gray-100',
                                        )}
                                    >
                                        {log.layer}
                                    </Badge>
                                </TableCell>
                                {/* Message */}
                                <TableCell className="font-mono">{log.message}</TableCell>
                                {/* Date */}
                                <TableCell className="font-mono text-right text-gray-600">
                                    {formatTimestamp(log.date)}
                                </TableCell>
                            </TableRow>
                        ))
                    )}
                </TableBody>
            </Table>
        </div>
    );
};

export default DetailLogSearchTable;