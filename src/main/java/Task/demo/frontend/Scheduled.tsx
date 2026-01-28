import React, { useState } from 'react';
import './Scheduled.css';

interface Flight {
  actual_arr_time?: string;
  scheduled_arr_time?: string;
  actual_dep_time?: string;
  scheduled_dep_time?: string;
  arr_delayed?: number;
  dep_delayed?: number;
  airline_iata?: string;
  flight_iata?: string;
  flight_number?: string;
  dep_iata?: string;
  arr_iata?: string;
  status?: string;
}

const Scheduled: React.FC = () => {
  const [iata, setIata] = useState('');
  const [arrivals, setArrivals] = useState<Flight[]>([]);
  const [departures, setDepartures] = useState<Flight[]>([]);
  
  // Backend API URL - ưu tiên lấy từ biến môi trường Vite nếu có
  const apiBaseUrl = (import.meta as any)?.env?.VITE_API_BASE_URL as string | undefined;
  const API_BASE_URL = apiBaseUrl && apiBaseUrl.trim().length > 0 ? apiBaseUrl.trim() : '';
  
  const getApiUrl = (endpoint: string) => {
    return `${API_BASE_URL}/api/schedules${endpoint}`;
  };

  // Fetch arrivals from backend
  const fetchArrivals = async (iataCode: string) => {
    try {
      const response = await fetch(getApiUrl(`/arrivals?iata=${iataCode}`));
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      
      const flights = await response.json();
      console.log('Received arrivals:', flights);
      setArrivals(flights || []);
    } catch (error) {
      console.error('Error fetching arrivals:', error);
      setArrivals([]);
    }
  };

  // Fetch departures from backend
  const fetchDepartures = async (iataCode: string) => {
    try {
      const response = await fetch(getApiUrl(`/departures?iata=${iataCode}`));
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      
      const flights = await response.json();
      console.log('Received departures:', flights);
      setDepartures(flights || []);
    } catch (error) {
      console.error('Error fetching departures:', error);
      setDepartures([]);
    }
  };

  // Get CSS class based on status
  const getStatusClass = (status?: string) => {
    if (!status) return '';
    const lowerStatus = status.toLowerCase();
    if (lowerStatus.includes('landed')) return 'landed';
    if (lowerStatus.includes('scheduled')) return 'scheduled';
    if (lowerStatus.includes('delayed')) return 'delayed';
    if (lowerStatus.includes('en-route') || lowerStatus.includes('active')) return 'en-route';
    return '';
  };

  // Handle refresh button click
  const handleRefresh = () => {
    const iataCode = iata.toUpperCase().trim();
    if (iataCode.length === 3) {
      fetchArrivals(iataCode);
      fetchDepartures(iataCode);
    } else {
      alert('Please enter a valid Airport IATA');
    }
  };

  // Handle input change
  const handleIataChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setIata(e.target.value.toUpperCase());
  };

  // Render arrivals table rows
  const renderArrivals = () => {
    if (!arrivals || arrivals.length === 0) {
      return (
        <tr>
          <td colSpan={5}>No data available</td>
        </tr>
      );
    }

    return arrivals.map((flight, index) => {
      const actualTime = flight.actual_arr_time || 'N/A';
      const scheduledTime = flight.scheduled_arr_time;
      const hasDelay = flight.arr_delayed && flight.arr_delayed > 0 && scheduledTime !== actualTime;

      return (
        <tr key={index}>
          <td>
            {actualTime}{' '}
            {hasDelay && <span className="old-time">{scheduledTime}</span>}
          </td>
          <td>{flight.airline_iata || 'N/A'}</td>
          <td>
            <a href="#" className="flight-number">
              {flight.flight_iata || flight.flight_number || 'N/A'}
            </a>
          </td>
          <td>{flight.dep_iata || 'N/A'}</td>
          <td className={`status ${getStatusClass(flight.status)}`}>
            {flight.status || 'N/A'}
          </td>
        </tr>
      );
    });
  };

  // Render departures table rows
  const renderDepartures = () => {
    if (!departures || departures.length === 0) {
      return (
        <tr>
          <td colSpan={5}>No data available</td>
        </tr>
      );
    }

    return departures.map((flight, index) => {
      const actualTime = flight.actual_dep_time || 'N/A';
      const scheduledTime = flight.scheduled_dep_time;
      const hasDelay = flight.dep_delayed && flight.dep_delayed > 0 && scheduledTime !== actualTime;

      return (
        <tr key={index}>
          <td>
            {actualTime}{' '}
            {hasDelay && <span className="old-time">{scheduledTime}</span>}
          </td>
          <td>{flight.airline_iata || 'N/A'}</td>
          <td>
            <a href="#" className="flight-number">
              {flight.flight_iata || flight.flight_number || 'N/A'}
            </a>
          </td>
          <td>{flight.arr_iata || 'N/A'}</td>
          <td className={`status ${getStatusClass(flight.status)}`}>
            {flight.status || 'N/A'}
          </td>
        </tr>
      );
    });
  };

  return (
    <div className="scheduled-container">
      <div className="header">
        <input
          type="text"
          className="iata"
          placeholder="KKK"
          value={iata}
          onChange={handleIataChange}
        />
        <button className="refresh-btn" onClick={handleRefresh}>
          Refresh
        </button>
    </div>

      <div className="container">
        <div className="board">
          <div className="board-header">
            <div className="status-dots">
              <span className="dot red"></span>
              <span className="dot yellow"></span>
              <span className="dot green"></span>
            </div>
            <span>Arrivals {iata}</span>
          </div>

          <table>
            <thead>
              <tr>
                <th>Time</th>
                <th>Carrier</th>
                <th>Flight</th>
                <th>Origin</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>{renderArrivals()}</tbody>
          </table>
        </div>

        <div className="board">
          <div className="board-header">
            <div className="status-dots">
              <span className="dot red"></span>
              <span className="dot yellow"></span>
              <span className="dot green"></span>
            </div>
            <span>Departures {iata}</span>
          </div>

          <table>
            <thead>
              <tr>
                <th>Time</th>
                <th>Carrier</th>
                <th>Flight</th>
                <th>Destination</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>{renderDepartures()}</tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default Scheduled;
