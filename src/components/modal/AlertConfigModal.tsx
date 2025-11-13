import { useEffect, useState } from 'react';
import {
    getAlertConfig,
    createAlertConfig,
    updateAlertConfig,
} from '@/services/alertService';
import type {
    GetAlertConfigResponse,
    PostAlertConfigParams,
    PutAlertConfigParams,
} from '@/types/alert';
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogDescription,
    DialogFooter,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { Switch } from '@/components/ui/switch';
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@/components/ui/select';
import { Input } from '@/components/ui/input';
import { Loader2 } from 'lucide-react';
import { toast } from 'sonner';

interface AlertConfigModalProps {
    projectUuid: string;
    open: boolean;
    onOpenChange: (open: boolean) => void;
}

const ALERT_TYPE_OPTIONS = [
    { value: 'ERROR_THRESHOLD', label: '개수 (에러 건수)' },
    { value: 'LATENCY', label: '시간 (응답 속도)' },
    { value: 'ERROR_RATE', label: '비율 (에러율)' },
];

export const AlertConfigModal = ({
    projectUuid,
    open,
    onOpenChange,
}: AlertConfigModalProps) => {
    // 상태관리
    const [isLoading, setIsLoading] = useState(true);
    const [isSaving, setIsSaving] = useState(false);
    const [config, setConfig] = useState<GetAlertConfigResponse | null>(null);

    // 폼 상태
    const [activeYN, setActiveYN] = useState(true);
    const [alertType, setAlertType] = useState<string>('ERROR_THRESHOLD');
    const [thresholdValue, setThresholdValue] = useState('');

    // 데이터 로드
    useEffect(() => {
        if (!open) { return; }

        const fetchConfig = async () => {
            setIsLoading(true);
            try {
                const data = await getAlertConfig({ projectUuid });
                setConfig(data);
                setActiveYN(data.activeYN === 'Y');
                setAlertType(data.alertType);
                setThresholdValue(String(data.thresholdValue));
            } catch (error) {
                console.error('알림 설정 조회 실패 (생성 모드로 진입):', error);
                setConfig(null);
                setActiveYN(true);
                setAlertType('ERROR_THRESHOLD');
                setThresholdValue('');
            } finally {
                setIsLoading(false);
            }
        };

        fetchConfig();
    }, [open, projectUuid]);

    // AlertType 바뀔 때 Threshold 초기화
    const handleAlertTypeChange = (newType: string) => {
        setAlertType(newType);
        setThresholdValue('');
    };

    // 저장/수정 핸들러
    const handleSave = async () => {
        setIsSaving(true);

        const activeYNString = activeYN ? 'Y' : 'N';
        const thresholdNum = Number(thresholdValue);

        if (activeYNString === 'Y') {
            // 유효성 검사
            if (isNaN(thresholdNum) || thresholdNum <= 0) {
                toast.error('유효한 임계값을 입력하세요.');
                setIsSaving(false);
                return;
            }
            if (alertType === 'ERROR_RATE' && thresholdNum > 100) {
                toast.error('비율은 100을 초과할 수 없습니다.');
                setIsSaving(false);
                return;
            }
        }

        try {
            if (config) {
                // 수정
                const payload: PutAlertConfigParams = {
                    alertConfigId: config.id,
                    alertType: alertType,
                    thresholdValue: thresholdNum,
                    activeYN: activeYNString,
                };
                await updateAlertConfig(payload);
                toast.success('알림 설정이 수정되었습니다.');
            } else {
                // 생성
                const payload: PostAlertConfigParams = {
                    projectUuid: projectUuid,
                    alertType: alertType,
                    thresholdValue: thresholdNum,
                    activeYN: activeYNString,
                };
                await createAlertConfig(payload);
                toast.success('알림 설정이 생성되었습니다.');
            }

            onOpenChange(false);
        } catch (error) {
            console.error('알림 설정 저장 실패:', error);
            toast.error('설정 저장에 실패했습니다.');
        } finally {
            setIsSaving(false);
        }
    };

    // 알림 조건
    const renderThresholdInput = () => {
        switch (alertType) {
            case 'ERROR_THRESHOLD':
                return (
                    <>
                        <Label htmlFor="threshold" className="text-right">
                            알림 조건 (개수)
                        </Label>
                        <Input
                            id="threshold"
                            type="number"
                            min={1}
                            value={thresholdValue}
                            onChange={e => setThresholdValue(e.target.value)}
                            className="col-span-2"
                            placeholder="예: 10 (10건 이상)"
                        />
                    </>
                );
            case 'ERROR_RATE':
                return (
                    <>
                        <Label htmlFor="threshold" className="text-right">
                            알림 조건 (비율)
                        </Label>
                        <Input
                            id="threshold"
                            type="number"
                            min={1}
                            max={100}
                            value={thresholdValue}
                            onChange={e => setThresholdValue(e.target.value)}
                            className="col-span-2"
                            placeholder="예: 5 (5% 이상)"
                        />
                    </>
                );
            case 'LATENCY':
                return (
                    <>
                        <Label htmlFor="threshold" className="text-right">
                            알림 조건 (시간)
                        </Label>
                        <Input
                            id="threshold"
                            className="col-span-2"
                            placeholder="응답 시간 (ms) - (추후 구현)"
                            disabled
                        />
                    </>
                );
            default:
                return null;
        }
    };

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="sm:max-w-[425px]">
                <DialogHeader>
                    <DialogTitle>알림 설정</DialogTitle>
                    <DialogDescription>
                        {config ? '알림 설정을 수정합니다.' : '새 알림 설정을 생성합니다.'}
                    </DialogDescription>
                </DialogHeader>

                {isLoading ? (
                    <div className="flex h-48 items-center justify-center">
                        <Loader2 className="h-8 w-8 animate-spin text-primary" />
                    </div>
                ) : (
                    <div className="grid gap-4 py-4">
                        {/* 알림 설정 여부 , 토글 */}
                        <div className="grid grid-cols-3 items-center gap-4">
                            <Label htmlFor="activeYN" className="text-right">
                                알림 수신 여부
                            </Label>
                            <Switch
                                id="activeYN"
                                checked={activeYN}
                                onCheckedChange={setActiveYN}
                                className="col-span-2"
                            />
                        </div>

                        {activeYN && (
                            <>
                                {/* 알림 타입 */}
                                <div className="grid grid-cols-3 items-center gap-4">
                                    <Label htmlFor="alertType" className="text-right">
                                        알림 타입
                                    </Label>
                                    <Select
                                        value={alertType}
                                        onValueChange={handleAlertTypeChange}
                                    >
                                        <SelectTrigger className="col-span-2">
                                            <SelectValue placeholder="알림 타입을 선택하세요" />
                                        </SelectTrigger>
                                        <SelectContent>
                                            {ALERT_TYPE_OPTIONS.map(opt => (
                                                <SelectItem key={opt.value} value={opt.value}>
                                                    {opt.label}
                                                </SelectItem>
                                            ))}
                                        </SelectContent>
                                    </Select>
                                </div>

                                {/* 조건 설정 */}
                                <div className="grid grid-cols-3 items-center gap-4">
                                    {renderThresholdInput()}
                                </div>
                            </>
                        )}

                    </div>
                )}

                <DialogFooter>
                    <Button
                        type="submit"
                        onClick={handleSave}
                        disabled={isLoading || isSaving}
                    >
                        {isSaving && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                        {isSaving
                            ? '저장 중...'
                            : config
                                ? '설정 수정'
                                : '설정 생성'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};