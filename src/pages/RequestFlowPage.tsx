import LogSearchBox from '@/components/LogSearchBox';
import FloatingChecklist from '@/components/FloatingChecklist';

const RequestFlowPage = () => {
  return (
    <div className="">
      <h1 className="font-godoM pb-5 text-xl text-gray-700">
        요청 흐름 시뮬레이션
      </h1>
      <LogSearchBox
        onSubmit={q => console.log('submit:', q)}
        onChange={q => console.log('change:', q)}
      />
      <FloatingChecklist />
    </div>
  );
};

export default RequestFlowPage;
