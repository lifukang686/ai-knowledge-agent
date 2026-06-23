import ReactDOM from 'react-dom/client'
import App from './App'
import './index.css'

window.addEventListener('unhandledrejection', (event) => {
  console.error('未处理的Promise拒绝:', event.reason);
  event.preventDefault();
});

window.addEventListener('error', (event) => {
  console.error('全局错误:', event.error, event.message);
});

ReactDOM.createRoot(document.getElementById('root')!).render(
  <App />,
)
