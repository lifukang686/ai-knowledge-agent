import React from 'react';
import { BrowserRouter } from 'react-router-dom';
import { AppRouter } from '@/router';
import { Toaster } from 'sonner';

function App() {
  return (
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
  );
}

export default App;