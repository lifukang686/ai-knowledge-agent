import React from 'react';
import { BrowserRouter } from 'react-router-dom';
import { AppRouter } from '@/router';
import { Toaster } from 'sonner';
import { ErrorBoundary } from '@/components/common/ErrorBoundary';

function App() {
  return (
    <ErrorBoundary>
      <BrowserRouter>
        <AppRouter />
        <Toaster
          position="top-right"
          expand={false}
          richColors
          closeButton
          duration={4000}
        />
      </BrowserRouter>
    </ErrorBoundary>
  );
}

export default App;