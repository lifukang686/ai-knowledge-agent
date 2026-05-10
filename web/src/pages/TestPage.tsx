import React from 'react';

export const TestPage: React.FC = () => {
  console.log('TestPage rendered!');
  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold text-blue-600">测试页面 - 知识库管理</h1>
      <p className="mt-4 text-gray-700">如果看到这个页面，说明路由正常工作！</p>
      <div className="mt-6 p-4 bg-green-100 rounded-lg">
        <p className="text-green-800">✅ 测试成功！</p>
      </div>
    </div>
  );
};

export default TestPage;
