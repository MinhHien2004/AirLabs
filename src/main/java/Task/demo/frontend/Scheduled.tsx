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
  
  // Backend API URL - ưu tiên lấy từ biến môi trường Vite nếu có (v1.0.2 - UI Update)
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
      {/* Hero Header */}
      <div className="hero-header">
        <div className="hero-content">
          <div className="hero-icon">✈</div>
          <h1 className="page-title">AirLabs Flight Tracker</h1>
          <p className="page-subtitle">Real-time arrivals & departures worldwide</p>
        </div>
      </div>

      {/* Search Section */}
      <div className="search-section">
        <div className="search-card">
          <div className="search-label">Airport IATA Code</div>
          <div className="search-row">
            <div className="input-wrapper">
              <span className="input-icon">🔍</span>
              <input
                type="text"
                className="iata"
                placeholder="e.g. SGN, HAN, NRT..."
                value={iata}
                onChange={handleIataChange}
                maxLength={3}
              />
            </div>
            <button className="refresh-btn" onClick={handleRefresh}>
              Search Flights
            </button>
          </div>
        </div>
      </div>

      {/* Flight Boards */}
      <div className="container">
        <div className="board">
          <div className="board-header arrivals-header">
            <div className="board-title">
              <span className="board-icon">🛬</span>
              <span>Arrivals</span>
            </div>
            <span className="board-count">{arrivals.length} flights</span>
            <span className="board-airport">{iata || '---'}</span>
          </div>

          <div className="board-body">
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
        </div>

        <div className="board">
          <div className="board-header departures-header">
            <div className="board-title">
              <span className="board-icon">🛫</span>
              <span>Departures</span>
            </div>
            <span className="board-count">{departures.length} flights</span>
            <span className="board-airport">{iata || '---'}</span>
          </div>

          <div className="board-body">
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

      {/* Flight Statistics */}
      {(arrivals.length > 0 || departures.length > 0) && (
        <div className="stats-section">
          <div className="stats-header">
            <div className="stats-header-line"></div>
            <h2 className="stats-title">Flight Analytics Dashboard</h2>
            <div className="stats-header-line"></div>
          </div>
          <div className="stats-grid">
            <div className="stat-card stat-total">
              <div className="stat-icon-box">
                <div className="stat-icon-symbol">Σ</div>
              </div>
              <div className="stat-content">
                <div className="stat-value">{arrivals.length + departures.length}</div>
                <div className="stat-label">Total Flights</div>
              </div>
            </div>
            <div className="stat-card stat-arrivals">
              <div className="stat-icon-box">
                <div className="stat-icon-symbol">↓</div>
              </div>
              <div className="stat-content">
                <div className="stat-value">{arrivals.length}</div>
                <div className="stat-label">Arrivals</div>
              </div>
            </div>
            <div className="stat-card stat-departures">
              <div className="stat-icon-box">
                <div className="stat-icon-symbol">↑</div>
              </div>
              <div className="stat-content">
                <div className="stat-value">{departures.length}</div>
                <div className="stat-label">Departures</div>
              </div>
            </div>
            <div className="stat-card stat-landed">
              <div className="stat-icon-box">
                <div className="stat-icon-symbol">●</div>
              </div>
              <div className="stat-content">
                <div className="stat-value">
                  {[...arrivals, ...departures].filter(f => f.status?.toLowerCase().includes('landed')).length}
                </div>
                <div className="stat-label">Landed</div>
              </div>
            </div>
            <div className="stat-card stat-scheduled-count">
              <div className="stat-icon-box">
                <div className="stat-icon-symbol">◷</div>
              </div>
              <div className="stat-content">
                <div className="stat-value">
                  {[...arrivals, ...departures].filter(f => f.status?.toLowerCase().includes('scheduled')).length}
                </div>
                <div className="stat-label">Scheduled</div>
              </div>
            </div>
            <div className="stat-card stat-enroute">
              <div className="stat-icon-box">
                <div className="stat-icon-symbol">⟿</div>
              </div>
              <div className="stat-content">
                <div className="stat-value">
                  {[...arrivals, ...departures].filter(f => {
                    const s = f.status?.toLowerCase() || '';
                    return s.includes('en-route') || s.includes('active');
                  }).length}
                </div>
                <div className="stat-label">En-Route</div>
              </div>
            </div>
            <div className="stat-card stat-delayed-count">
              <div className="stat-icon-box">
                <div className="stat-icon-symbol">⧗</div>
              </div>
              <div className="stat-content">
                <div className="stat-value">
                  {[...arrivals, ...departures].filter(f => f.status?.toLowerCase().includes('delayed')).length}
                </div>
                <div className="stat-label">Delayed</div>
              </div>
            </div>
            <div className="stat-card stat-cancelled">
              <div className="stat-icon-box">
                <div className="stat-icon-symbol">✕</div>
              </div>
              <div className="stat-content">
                <div className="stat-value">
                  {[...arrivals, ...departures].filter(f => f.status?.toLowerCase().includes('cancelled')).length}
                </div>
                <div className="stat-label">Cancelled</div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Footer */}
      <div className="app-footer">
        <span>Powered by AirLabs API</span>
        <span className="footer-dot">•</span>
        <span>v2.0</span>
      </div>
    </div>
  );
};

export default Scheduled;
