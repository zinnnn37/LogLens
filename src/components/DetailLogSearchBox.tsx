import { useState, useEffect } from 'react';
import {
  Search,
  ChevronDown,
  CalendarIcon,
  ArrowDownUp,
  Clock,
  RotateCw,
} from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  DropdownMenu,
  DropdownMenuCheckboxItem,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { IMaskInput } from 'react-imask';
import { cn } from '@/lib/utils';

// ===== 타입 =====
export interface SearchCriteria {
  traceId: string;
  keyword: string;
  sourceType: string[];
  logLevel: string[];
  startTime: string; // ISO: 'YYYY-MM-DDTHH:mm:ss'
  endTime: string; // ISO: 'YYYY-MM-DDTHH:mm:ss'
  sort: string; // ex) 'TIMESTAMP,DESC'
}

interface DetailLogSearchBoxProps {
  onSearch: (criteria: SearchCriteria) => void;
  initialKeyword?: string | null;
  initialLogLevel?: string[];
}

// ===== 옵션 =====
const SOURCE_TYPE_OPTIONS = [
  { id: 'FE', label: 'FRONT' },
  { id: 'BE', label: 'BACK' },
  { id: 'INFRA', label: 'INFRA' },
];

const LOG_LEVEL_OPTIONS = [
  { id: 'INFO', label: 'INFO' },
  { id: 'WARN', label: 'WARN' },
  { id: 'ERROR', label: 'ERROR' },
];

const DetailLogSearchBox = ({
  onSearch,
  initialKeyword,
  initialLogLevel,
}: DetailLogSearchBoxProps) => {
  // 검색어/타입
  const [searchType, setSearchType] = useState<'traceId' | 'keyword'>(
    'traceId',
  );
  const [searchValue, setSearchValue] = useState('');

  // 필터
  const [sourceType, setSourceType] = useState<string[]>([]);
  const [logLevel, setLogLevel] = useState<string[]>([]);

  // 시간(분리 입력 → ISO 조합)
  const [startDate, setStartDate] = useState(''); // YYYY-MM-DD
  const [startClock, setStartClock] = useState(''); // HH:mm:ss
  const [endDate, setEndDate] = useState('');
  const [endClock, setEndClock] = useState(''); // HH:mm:ss

  // 정렬
  const [sort, setSort] = useState('TIMESTAMP,DESC');

  // 초기값 설정 여부 추적
  const [isInitialized, setIsInitialized] = useState(false);

  // initialKeyword와 initialLogLevel로 상태 초기화
  useEffect(() => {
    if (
      !isInitialized &&
      (initialKeyword || (initialLogLevel && initialLogLevel.length > 0))
    ) {
      if (initialKeyword) {
        setSearchType('keyword');
        setSearchValue(initialKeyword);
      }

      if (initialLogLevel && initialLogLevel.length > 0) {
        setLogLevel(initialLogLevel);
      }

      setIsInitialized(true);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [initialKeyword, initialLogLevel]);

  // 상태가 초기화된 후 검색 실행
  useEffect(() => {
    if (isInitialized) {
      const startISO = composeISO(startDate, startClock);
      const endISO = composeISO(endDate, endClock);

      onSearch({
        traceId: searchType === 'traceId' ? searchValue : '',
        keyword: searchType === 'keyword' ? searchValue : '',
        sourceType,
        logLevel,
        startTime: startISO,
        endTime: endISO,
        sort,
      });

      setIsInitialized(false); // 한 번만 실행되도록
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isInitialized]);

  // ===== helpers =====
  // date + clock → 'YYYY-MM-DDTHH:mm:ss'
  const composeISO = (date: string, clock: string) => {
    if (!date || !clock || clock.length < 8) {
      return '';
    }
    return `${date}T${clock}`;
  };

  const getSearchProps = () => {
    if (searchType === 'traceId') {
      return { icon: Search, placeholder: 'TraceID로 검색...' };
    }
    return { icon: Search, placeholder: '키워드로 검색...' };
  };
  const { icon: SearchIcon, placeholder } = getSearchProps();

  const getDropdownButtonText = (label: string, values: string[]) => {
    if (values.length === 0) {
      return `${label}`;
    }
    if (values.length > 1) {
      return `${label} (${values.length}개)`;
    }
    const allOptions = [...SOURCE_TYPE_OPTIONS, ...LOG_LEVEL_OPTIONS];
    return allOptions.find(opt => opt.id === values[0])?.label || values[0];
  };

  // ===== 검색 실행 =====
  const handleSearch = () => {
    const startISO = composeISO(startDate, startClock);
    const endISO = composeISO(endDate, endClock);

    onSearch({
      traceId: searchType === 'traceId' ? searchValue : '',
      keyword: searchType === 'keyword' ? searchValue : '',
      sourceType,
      logLevel,
      startTime: startISO,
      endTime: endISO,
      sort,
    });
  };

  return (
    <div className="flex w-full flex-nowrap items-center gap-2 rounded-lg border bg-white p-4 shadow-sm">
      {/* 검색 타입 */}
      <div className="relative min-w-[350px] flex-1">
        <SearchIcon className="absolute top-1/2 left-3 h-4 w-4 -translate-y-1/2 text-gray-400" />
        <Input
          type="text"
          placeholder={placeholder}
          className="pr-[120px] pl-10"
          value={searchValue}
          onChange={e => setSearchValue(e.target.value)}
        />
        <div className="absolute top-0 right-2 flex h-full items-center">
          <Select
            value={searchType}
            onValueChange={(v: 'traceId' | 'keyword') => setSearchType(v)}
          >
            <SelectTrigger className="w-auto border-0 bg-transparent text-xs text-gray-600 shadow-none focus:ring-0">
              <SelectValue placeholder="검색 타입" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="traceId">TraceID</SelectItem>
              <SelectItem value="keyword">Keyword</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      {/* 시스템 필터 */}
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button
            variant="outline"
            className="w-auto min-w-[120px] justify-between"
          >
            <span>{getDropdownButtonText('시스템', sourceType)}</span>
            <ChevronDown className="h-4 w-4 opacity-50" />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent className="w-[140px]">
          {SOURCE_TYPE_OPTIONS.map(option => (
            <DropdownMenuCheckboxItem
              key={option.id}
              checked={sourceType.includes(option.id)}
              onCheckedChange={(checked: boolean) => {
                setSourceType(prev =>
                  checked
                    ? [...prev, option.id]
                    : prev.filter(id => id !== option.id),
                );
              }}
              onSelect={e => e.preventDefault()}
            >
              {option.label}
            </DropdownMenuCheckboxItem>
          ))}
        </DropdownMenuContent>
      </DropdownMenu>

      {/* 레벨 필터 */}
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button
            variant="outline"
            className="w-auto min-w-[120px] justify-between"
          >
            <span>{getDropdownButtonText('레벨', logLevel)}</span>
            <ChevronDown className="h-4 w-4 opacity-50" />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent className="w-[140px]">
          {LOG_LEVEL_OPTIONS.map(option => (
            <DropdownMenuCheckboxItem
              key={option.id}
              checked={logLevel.includes(option.id)}
              onCheckedChange={(checked: boolean) => {
                setLogLevel(prev =>
                  checked
                    ? [...prev, option.id]
                    : prev.filter(id => id !== option.id),
                );
              }}
              onSelect={e => e.preventDefault()}
            >
              {option.label}
            </DropdownMenuCheckboxItem>
          ))}
        </DropdownMenuContent>
      </DropdownMenu>

      {/* 기간 설정 */}
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button
            variant="outline"
            className="w-auto min-w-[160px] justify-between"
          >
            <CalendarIcon className="mr-2 h-4 w-4 opacity-50" />
            <span>
              {startDate || startClock || endDate || endClock
                ? '기간 설정됨'
                : '기간 설정'}
            </span>
          </Button>
        </DropdownMenuTrigger>

        <DropdownMenuContent
          className="relative w-auto p-4"
          onSelect={e => e.preventDefault()}
        >
          <Button
            variant="ghost"
            size="sm"
            className="text-muted-foreground absolute top-2 right-2 h-auto px-2 py-1 text-xs"
            onClick={() => {
              setStartDate('');
              setStartClock('');
              setEndDate('');
              setEndClock('');
            }}
          >
            <RotateCw className="mr-1 h-3 w-3" />
            초기화
          </Button>

          <div className="mt-6 grid gap-4">
            {/* 시작 */}
            <div className="grid w-full max-w-sm gap-1.5">
              <Label htmlFor="startDate" className="text-sm font-medium">
                시작 시간
              </Label>
              <div className="flex gap-2">
                <Input
                  id="startDate"
                  type="date"
                  value={startDate}
                  onChange={e => setStartDate(e.target.value)}
                  className="w-[150px]"
                />
                <div className="relative w-[130px]">
                  <IMaskInput
                    mask="HH:mm:ss"
                    blocks={{
                      HH: { mask: '00', placeholderChar: '_' },
                      mm: { mask: '00', placeholderChar: '_' },
                      ss: { mask: '00', placeholderChar: '_' },
                    }}
                    id="startClock"
                    placeholder="HH:mm:ss"
                    className={cn(
                      'border-input bg-background ring-offset-background placeholder:text-muted-foreground focus-visible:ring-ring flex h-10 w-full rounded-md border px-3 py-2 text-sm file:border-0 file:bg-transparent file:text-sm file:font-medium focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:outline-none disabled:cursor-not-allowed disabled:opacity-50',
                      'pl-8',
                    )}
                    value={startClock}
                    onAccept={value => setStartClock(value as string)}
                  />
                  <Clock className="absolute top-1/2 left-2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                </div>
              </div>
            </div>

            {/* 종료 */}
            <div className="grid w-full max-w-sm gap-1.5">
              <Label htmlFor="endDate" className="text-sm font-medium">
                종료 시간
              </Label>
              <div className="flex gap-2">
                <Input
                  id="endDate"
                  type="date"
                  value={endDate}
                  onChange={e => setEndDate(e.target.value)}
                  className="w-[150px]"
                />
                <div className="relative w-[130px]">
                  <IMaskInput
                    mask="HH:mm:ss"
                    blocks={{
                      HH: { mask: '00', placeholderChar: '_' },
                      mm: { mask: '00', placeholderChar: '_' },
                      ss: { mask: '00', placeholderChar: '_' },
                    }}
                    id="endClock"
                    placeholder="HH:mm:ss"
                    className={cn(
                      'border-input bg-background ring-offset-background placeholder:text-muted-foreground focus-visible:ring-ring flex h-10 w-full rounded-md border px-3 py-2 text-sm file:border-0 file:bg-transparent file:text-sm file:font-medium focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:outline-none disabled:cursor-not-allowed disabled:opacity-50',
                      'pl-8',
                    )}
                    value={endClock}
                    onAccept={value => setEndClock(value as string)}
                  />
                  <Clock className="absolute top-1/2 left-2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                </div>
              </div>
            </div>
          </div>
        </DropdownMenuContent>
      </DropdownMenu>

      {/* 정렬 */}
      <Select value={sort} onValueChange={setSort}>
        <SelectTrigger className="w-auto min-w-[140px]">
          <ArrowDownUp className="mr-2 h-4 w-4 text-gray-500" />
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="TIMESTAMP,DESC">최신순</SelectItem>
          <SelectItem value="TIMESTAMP,ASC">오래된순</SelectItem>
        </SelectContent>
      </Select>

      {/* 검색 버튼 */}
      <Button
        onClick={handleSearch}
        className="bg-primary text-white hover:bg-blue-500"
      >
        검색
      </Button>
    </div>
  );
};

export default DetailLogSearchBox;
