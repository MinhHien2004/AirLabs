import React from 'react';
import Scheduled from './Scheduled';
import './index.css';

function App() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100">
      <header className="bg-gradient-to-r from-blue-600 to-indigo-700 text-white shadow-lg">
        <div className="container mx-auto px-4 py-6">
          <h1 className="text-3xl font-bold text-center">
            ✈️ AirLabs Real-time Flight Tracker
          </h1>
          <p className="text-center text-blue-100 mt-2">
            Track arrivals and departures in real-time | v2.0
          </p>
        </div>
      </header>
      <Scheduled />
    </div>
  );
}

export default App;
