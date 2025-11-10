// src/pages/RequestFlowPage.tsx

import { useState } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import LogSearchBox from '@/components/LogSearchBox';
import FloatingChecklist from '@/components/FloatingChecklist';

const RequestFlowPage = () => {
  const { projectUuid } = useParams<{ projectUuid: string }>();

  const [searchParams] = useSearchParams();

  const initialTraceId = searchParams.get('traceId');

  const [query, setQuery] = useState(initialTraceId ?? '');

  // TODO: projectUuid를 사용해서 실제 프로젝트 요청 흐름 데이터 가져오기
  console.log('Current project UUID:', projectUuid);

  const handleSearchSubmit = (traceId: string) => {
    console.log('submit:', traceId);
  };

  return (
    <div className="">
      <h1 className="font-godoM pb-5 text-xl text-gray-700">
        요청 흐름 시뮬레이션
      </h1>
      <LogSearchBox
        value={query}
        onChange={setQuery}
        onSubmit={handleSearchSubmit}
      />
      <FloatingChecklist />
    </div>
  );
};

export default RequestFlowPage;
