import DependencyGraph from "@/components/DependencyGraph";

const DependencyGraphPage = () => {
  return (
    <div className="font-pretendard space-y-6 p-6 py-1">
      <h1 className="font-godoM text-lg">의존성 그래프</h1>

      <DependencyGraph></DependencyGraph>
    </div>);

};

export default DependencyGraphPage;
