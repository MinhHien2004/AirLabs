import React, { useState, useEffect } from 'react';

interface Flight {
  id: number;
  airline_iata: string;
  airline_icao: string | null;
  flight_iata: string ;
  flight_icao: string | null;
  flight_number: string;
  dep_iata: string;
  dep_icao: string | null;
  dep_terminal: string | null;
  dep_gate: string | null;
  dep_time: string;
  dep_time_utc: string;
  arr_iata: string;
  arr_icao: string | null;
  arr_terminal: string | null;
  arr_gate: string | null;
  arr_baggage: string | null;
  arr_time: string;
  arr_time_utc: string;
  cs_airline_iata: string | null;
  cs_flight_number: string | null;
  cs_flight_iata: string | null;
  status: string;
  duration: number;
  delayed: number | null;
  dep_delayed: number | null;
  arr_delayed: number | null;
  aircraft_icao: string | null;
  arr_time_ts: number;
  dep_time_ts: number;
}

const FlightsInfo: React.FC = () => {
  const [flights, setFlights] = useState<Flight[]>([]);
  const [selectedDepIata, setSelectedDepIata] = useState<string>('all');

  const [searchTerm, setSearchTerm] = useState<string>('');
  const [modalMode, setModalMode] = useState<'add' | 'edit' | 'show'>('add');
  const [currentFlight, setCurrentFlight] = useState<Flight | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);

  const [formData, setFormData] = useState<Partial<Flight>>({
    airline_iata: '',
    flight_number: '',
    dep_iata: '',
    dep_icao: '',
    dep_terminal: '',
    dep_gate: '',
    dep_time: '',
    dep_time_utc: '',
    arr_iata: '',
    arr_icao: '',
    arr_terminal: '',
    arr_gate: '',
    arr_baggage: '',
    arr_time: '',
    arr_time_utc: '',
    status: 'Scheduled',
    duration: 0,
    delayed: null,
    dep_delayed: null,
    arr_delayed: null
  })

  // Lấy flights khi selectedDepIata thay đổi
  useEffect(() => {
    fetchFlights();
  }, [selectedDepIata]);

  const fetchFlights = () => {
    if (selectedDepIata === 'all') {
      fetch('http://localhost:8080/api/flights')
        .then(response => response.json())
        .then(data => setFlights(data))
        .catch(error => console.error('Error fetching flights:', error));
    } else {
      fetch(`http://localhost:8080/api/airlabs/schedules?dep_iata=${selectedDepIata}`)
        .then(response => response.json())
        .then(data => {
          const flightsData = data.response || [];
          setFlights(flightsData);
        }
        )
        .catch(error => console.error('Error fetching flights:', error));
    }
  };


  const handleSearch = () => {
    if(searchTerm.trim() === '' || searchTerm.toLowerCase() === 'all'){
      setSelectedDepIata('all');
    } else {
      setSelectedDepIata(searchTerm.toUpperCase());
    }
  }

  const handlePress = (e: React.KeyboardEvent) => {
    if(e.key === "Enter") {
      handleSearch();
    }
  }

  const getStatusClass = (status: string): string => {
    switch (status) {
      case 'active':
        return 'text-green-600 font-semibold';
      case 'landed':
        return 'text-red-600 font-semibold';
      case 'scheduled':
        return 'text-yellow-600 font-semibold';
      default:
        return 'text-gray-600';
    }
  };

  const handleShow = (flight: Flight) => {
    setModalMode('show');
    setCurrentFlight(flight);
    setIsModalOpen(true);
  };
  const handleAdd = () => {
    setModalMode('add');
    setFormData({
      airline_iata: '',
      airline_icao: '',
      flight_iata: '',
      flight_icao: '',
      flight_number: '',
      dep_iata: '',
      dep_icao: '',
      dep_terminal: '',
      dep_gate: '',
      dep_time: '',
      dep_time_utc: '',
      arr_iata: '',
      arr_icao: '',
      arr_terminal: '',
      arr_gate: '',
      arr_baggage: '',
      arr_time: '',
      arr_time_utc: '',
      status: 'Scheduled',
      duration: 0,
      delayed: null,
      dep_delayed: null,
      arr_delayed: null
    });
    setIsModalOpen(true);
  }

  const handleEdit = (flight: Flight) => {
    setModalMode('edit');
    setCurrentFlight(flight);
    setFormData({
      airline_iata: flight.airline_iata,
      airline_icao: flight.airline_icao,
      flight_iata: flight.flight_iata,
      flight_icao: flight.flight_icao,
      flight_number: flight.flight_number,
      dep_iata: flight.dep_iata,
      dep_icao: flight.dep_icao,
      dep_terminal: flight.dep_terminal,
      dep_gate: flight.dep_gate,
      dep_time: flight.dep_time,
      dep_time_utc: flight.dep_time_utc,
      arr_iata: flight.arr_iata,
      arr_icao: flight.arr_icao,
      arr_terminal: flight.arr_terminal,
      arr_gate: flight.arr_gate,
      arr_baggage: flight.arr_baggage,
      arr_time: flight.arr_time,
      arr_time_utc: flight.arr_time_utc,
      cs_airline_iata: flight.cs_airline_iata,
      cs_flight_number: flight.cs_flight_number,
      cs_flight_iata: flight.cs_flight_iata,
      status: flight.status,
      duration: flight.duration,
      delayed: flight.delayed,
      dep_delayed: flight.dep_delayed,
      arr_delayed: flight.arr_delayed,
      aircraft_icao: flight.aircraft_icao
    });
    setIsModalOpen(true);
  }

  const handleDelete = (id: number) => {
    if (window.confirm("Are you sure you want to delete this flight?")) {
      fetch(`http://localhost:8080/api/flights/${id}`, {
        method: "DELETE"
      })
        .then(() => {
          fetchFlights();
          alert("Flight deleted successfully!");
        })
        .catch(error => {
          console.error("Error deleting flight:", error);
          alert("Error deleting flight!");
        })
    }
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    const method = modalMode === 'add' ? 'POST' : 'PUT';
    const url = modalMode === 'add' ? 'http://localhost:8080/api/flights' : `http://localhost:8080/api/flights/${currentFlight?.id}`;
    
    // Fix datetime format: append :00 if missing
    const payload = {
      ...formData,
      dep_time: formData.dep_time && formData.dep_time.length === 16 ? formData.dep_time + ':00' : formData.dep_time,
      dep_time_utc: formData.dep_time_utc && formData.dep_time_utc.length === 16 ? formData.dep_time_utc + ':00' : formData.dep_time_utc,
      arr_time: formData.arr_time && formData.arr_time.length === 16 ? formData.arr_time + ':00' : formData.arr_time,
      arr_time_utc: formData.arr_time_utc && formData.arr_time_utc.length === 16 ? formData.arr_time_utc + ':00' : formData.arr_time_utc
    };
    
    console.log('Submitting flight:', method, payload);
    
    fetch(url, {
      method: method,
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(payload)
    })
      .then(response => {
        if (!response.ok) {
          return response.json().then(err => {
            console.error('Server error:', err);
            throw new Error(JSON.stringify(err));
          });
        }
        return response.json();
      })
      .then(() => {
        fetchFlights();
        setIsModalOpen(false);
        alert(`Flight ${modalMode === 'add' ? 'added' : 'updated'} successfully!`);
      })
      .catch(error => {
        console.error(`Error ${modalMode === 'add' ? 'adding' : 'updating'} flight:`, error);
        alert(`Error ${modalMode === 'add' ? 'adding' : 'updating'} flight! Check console for details.`);
      });
  }

  return (
    <div className="max-w-7xl mx-auto p-6 font-sans">
      <h1 className="text-3xl font-bold text-gray-800 text-center mb-8">
        Flights Information
      </h1>

      {/* Searchable Dropdown cho dep_iata */}
      <div className="mb-6 p-4 bg-gray-100 rounded-lg flex items-center gap-3">
        <label htmlFor="dep-iata-search" className="font-semibold text-gray-700">
          Enter departure airport code:
        </label>
        <div className="relative w-64 z-50">
          <input
            id="dep-iata-input"
            type="text"
            value={searchTerm}
            onChange={(e) => {
              setSearchTerm(e.target.value);
            }}
            onKeyPress={handlePress}
            placeholder="Enter IATA code (e.g., HAN, SGN)"
            className="w-full px-4 py-2 border-2 border-gray-300 rounded-md text-sm bg-white hover:border-blue-500 focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-200 transition-all"
          />
        </div>
        <button
          onClick={handleSearch}
          className='px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors font-semibold'
        >
          Search
        </button>
        <button
          onClick={handleAdd}
          className="px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 transition-colors font-semibold"
        >
          Add Flight
        </button>
      </div>



      {/* Bảng hiển thị thông tin */}
      <div className="overflow-x-auto shadow-lg rounded-lg max-h-[600px] overflow-y-auto">
        <table className="w-full bg-white border-collapse">
          <thead className="bg-blue-600 text-white sticky top-0 z-10">
            <tr>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider">
                Flight ID
              </th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider">
                Airline
              </th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider">
                Flight Number
              </th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider">
                Departure
              </th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider">
                Dep Time
              </th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider">
                Arrival
              </th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider">
                Arr Time
              </th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider">
                Status
              </th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider">
                Action
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {flights.length > 0 ? (
              flights.map(flight => (
                <tr key={flight.id} className="hover:bg-gray-50 transition-colors">
                  <td className="px-4 py-3 text-gray-700">{flight.id}</td>
                  <td className="px-4 py-3 text-gray-700">{flight.airline_iata}</td>
                  <td className="px-4 py-3 text-gray-700">{flight.flight_number}</td>
                  <td className="px-4 py-3 text-gray-700">{flight.dep_iata}</td>
                  <td className="px-4 py-3 text-gray-700">{flight.dep_time}</td>
                  <td className="px-4 py-3 text-gray-700">{flight.arr_iata}</td>
                  <td className="px-4 py-3 text-gray-700">{flight.arr_time}</td>
                  <td className={`px-4 py-3 ${getStatusClass(flight.status)}`}>
                    {flight.status}
                  </td>
                  <td className="px-4 py-3">
                    <button
                      onClick={() => handleShow(flight)}
                      className="px-3 py-1 bg-blue-500 text-white rounded hover:bg-blue-600 transition-colors text-sm"
                    >
                      Show
                    </button>
                    <button
                      onClick={() => handleEdit(flight)}
                      className="px-3 py-1 bg-blue-500 text-white rounded hover:bg-blue-600 transition-colors text-sm"
                    >
                      Edit
                    </button>
                    <button
                      onClick={() => handleDelete(flight.id)}
                      className="px-3 py-1 bg-red-500 text-white rounded hover:bg-red-600 transition-colors text-sm"
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan={8} className="px-4 py-6 text-center text-gray-500">
                  Không có dữ liệu
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Modal Show/Add/Edit */}
      {isModalOpen && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg w-full max-w-4xl max-h-[90vh] overflow-y-auto">
            {/* Modal Header */}
            <div className="sticky top-0 bg-white border-b px-6 py-4 flex justify-between items-center">
              <h2 className="text-2xl font-bold text-gray-800">
                {modalMode === 'show' ? 'Flight Details' : modalMode === 'add' ? 'Add New Flight' : 'Edit Flight'}
              </h2>
              <button
                onClick={() => setIsModalOpen(false)}
                className="text-gray-500 hover:text-gray-700 text-2xl font-bold"
              >
                ×
              </button>
            </div>

            {/* Modal Body */}
            <div className="px-6 py-4">
              {modalMode === 'show' && currentFlight ? (
                // View Mode - Chi tiết flight
                <div className="space-y-6">
                  {/* Flight Header */}
                  <div className="bg-gradient-to-r from-blue-500 to-blue-600 text-white p-6 rounded-lg">
                    <div className="flex justify-between items-start">
                      <div>
                        <h3 className="text-3xl font-bold mb-2">
                          {currentFlight.airline_iata} {currentFlight.flight_number}
                        </h3>
                        <p className="text-blue-100">Flight ID: #{currentFlight.id}</p>
                      </div>
                      <span className={`px-4 py-2 rounded-full font-semibold border-2 ${getStatusClass(currentFlight.status)}`}>
                        {currentFlight.status}
                      </span>
                    </div>
                  </div>

                  {/* Flight Route */}
                  <div className="grid grid-cols-3 gap-4 items-center">
                    <div className="text-center p-4 bg-blue-50 rounded-lg">
                      <p className="text-sm text-gray-600 mb-1">Departure</p>
                      <p className="text-3xl font-bold text-blue-600">{currentFlight.dep_iata}</p>
                      <p className="text-xs text-gray-500 mt-1">{currentFlight.dep_icao}</p>
                    </div>
                    <div className="flex flex-col items-center">
                      <svg className="w-16 h-16 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7l5 5m0 0l-5 5m5-5H6" />
                      </svg>
                      <p className="text-sm text-gray-600 mt-2">{currentFlight.duration} minutes</p>
                    </div>
                    <div className="text-center p-4 bg-green-50 rounded-lg">
                      <p className="text-sm text-gray-600 mb-1">Arrival</p>
                      <p className="text-3xl font-bold text-green-600">{currentFlight.arr_iata}</p>
                      <p className="text-xs text-gray-500 mt-1">{currentFlight.arr_icao}</p>
                    </div>
                  </div>

                  {/* Departure Info */}
                  <div className="border rounded-lg p-4">
                    <h4 className="text-lg font-semibold text-gray-800 mb-3 flex items-center">
                      <svg className="w-5 h-5 mr-2 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 10l7-7m0 0l7 7m-7-7v18" />
                      </svg>
                      Departure Information
                    </h4>
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <p className="text-sm text-gray-600">Local Time</p>
                        <p className="font-semibold text-gray-800">{currentFlight.dep_time}</p>
                      </div>
                      <div>
                        <p className="text-sm text-gray-600">UTC Time</p>
                        <p className="font-semibold text-gray-800">{currentFlight.dep_time_utc}</p>
                      </div>
                      <div>
                        <p className="text-sm text-gray-600">Terminal</p>
                        <p className="font-semibold text-gray-800">{currentFlight.dep_terminal || 'N/A'}</p>
                      </div>
                      <div>
                        <p className="text-sm text-gray-600">Gate</p>
                        <p className="font-semibold text-gray-800">{currentFlight.dep_gate || 'N/A'}</p>
                      </div>
                      {currentFlight.dep_delayed !== null && (
                        <div className="col-span-2">
                          <p className="text-sm text-gray-600">Delayed</p>
                          <p className="font-semibold text-red-600">{currentFlight.dep_delayed} minutes</p>
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Arrival Info */}
                  <div className="border rounded-lg p-4">
                    <h4 className="text-lg font-semibold text-gray-800 mb-3 flex items-center">
                      <svg className="w-5 h-5 mr-2 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 14l-7 7m0 0l-7-7m7 7V3" />
                      </svg>
                      Arrival Information
                    </h4>
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <p className="text-sm text-gray-600">Local Time</p>
                        <p className="font-semibold text-gray-800">{currentFlight.arr_time}</p>
                      </div>
                      <div>
                        <p className="text-sm text-gray-600">UTC Time</p>
                        <p className="font-semibold text-gray-800">{currentFlight.arr_time_utc}</p>
                      </div>
                      <div>
                        <p className="text-sm text-gray-600">Terminal</p>
                        <p className="font-semibold text-gray-800">{currentFlight.arr_terminal || 'N/A'}</p>
                      </div>
                      <div>
                        <p className="text-sm text-gray-600">Gate</p>
                        <p className="font-semibold text-gray-800">{currentFlight.arr_gate || 'N/A'}</p>
                      </div>
                      <div>
                        <p className="text-sm text-gray-600">Baggage</p>
                        <p className="font-semibold text-gray-800">{currentFlight.arr_baggage || 'N/A'}</p>
                      </div>
                      {currentFlight.arr_delayed !== null && (
                        <div>
                          <p className="text-sm text-gray-600">Delayed</p>
                          <p className="font-semibold text-red-600">{currentFlight.arr_delayed} minutes</p>
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Additional Info */}
                  <div className="border rounded-lg p-4 bg-gray-50">
                    <h4 className="text-lg font-semibold text-gray-800 mb-3">Additional Information</h4>
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <p className="text-sm text-gray-600">Aircraft</p>
                        <p className="font-semibold text-gray-800">{currentFlight.aircraft_icao || 'N/A'}</p>
                      </div>
                      <div>
                        <p className="text-sm text-gray-600">Flight IATA/ICAO</p>
                        <p className="font-semibold text-gray-800">
                          {currentFlight.flight_iata || 'N/A'} / {currentFlight.flight_icao || 'N/A'}
                        </p>
                      </div>
                      {currentFlight.delayed !== null && (
                        <div>
                          <p className="text-sm text-gray-600">Total Delayed</p>
                          <p className="font-semibold text-red-600">{currentFlight.delayed} minutes</p>
                        </div>
                      )}
                      {currentFlight.cs_airline_iata && (
                        <div className="col-span-2">
                          <p className="text-sm text-gray-600">Codeshare</p>
                          <p className="font-semibold text-gray-800">
                            {currentFlight.cs_airline_iata} {currentFlight.cs_flight_number} ({currentFlight.cs_flight_iata})
                          </p>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              ) : (
                // Edit/Add Mode - Form
                <form onSubmit={handleSubmit}>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-1">
                        Airline IATA *
                      </label>
                      <input
                        type="text"
                        value={formData.airline_iata}
                        onChange={(e) => setFormData({ ...formData, airline_iata: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        required
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-1">
                        Airline ICAO
                      </label>
                      <input
                        type="text"
                        value={formData.airline_icao || ''}
                        onChange={(e) => setFormData({ ...formData, airline_icao: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-1">
                        Flight IATA *
                      </label>
                      <input
                        type="text"
                        value={formData.flight_iata}
                        onChange={(e) => setFormData({ ...formData, flight_iata: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        required
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-1">
                        Flight ICAO
                      </label>
                      <input
                        type="text"
                        value={formData.flight_icao || ''}
                        onChange={(e) => setFormData({ ...formData, flight_icao: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-1">
                        Flight Number *
                      </label>
                      <input
                        type="text"
                        value={formData.flight_number}
                        onChange={(e) => setFormData({ ...formData, flight_number: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        required
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-1">
                        Departure IATA *
                      </label>
                      <input
                        type="text"
                        value={formData.dep_iata}
                        onChange={(e) => setFormData({ ...formData, dep_iata: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        required
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-1">
                        Departure ICAO
                      </label>
                      <input
                        type="text"
                        value={formData.dep_icao || ''}
                        onChange={(e) => setFormData({ ...formData, dep_icao: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-1">
                        Departure Terminal
                      </label>
                      <input
                        type="text"
                        value={formData.dep_terminal || ''}
                        onChange={(e) => setFormData({ ...formData, dep_terminal: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                      />
                    </div>

                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-1">
                        Departure Gate
                      </label>
                      <input
                        type="text"
                        value={formData.dep_gate || ''}
                        onChange={(e) => setFormData({ ...formData, dep_gate: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-1">
                        Departure Time *
                      </label>
                      <input
                        type="datetime-local"
                        value={formData.dep_time}
                        onChange={(e) => setFormData({ ...formData, dep_time: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        required
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-1">
                        Departure Time UTC*
                      </label>
                      <input
                        type="datetime-local"
                        value={formData.dep_time_utc}
                        onChange={(e) => setFormData({ ...formData, dep_time_utc: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        required
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-1">
                        Arrival IATA *
                      </label>
                      <input
                        type="text"
                        value={formData.arr_iata}
                        onChange={(e) => setFormData({ ...formData, arr_iata: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        required
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-1">
                        Arrival ICAO 
                      </label>
                      <input
                        type="text"
                        value={formData.arr_icao || ''}
                        onChange={(e) => setFormData({ ...formData, arr_icao: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-1">
                        Arrival Terminal
                      </label>
                      <input
                        type="text"
                        value={formData.arr_terminal || ''}
                        onChange={(e) => setFormData({ ...formData, arr_terminal: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-1">
                        Arrival Gate
                      </label>
                      <input
                        type="text"
                        value={formData.arr_gate || ''}
                        onChange={(e) => setFormData({ ...formData, arr_gate: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-1">
                        Arrival Baggage
                      </label>
                      <input
                        type="text"
                        value={formData.arr_baggage || ''}
                        onChange={(e) => setFormData({ ...formData, arr_baggage: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-1">
                        Arrival Time *
                      </label>
                      <input
                        type="datetime-local"
                        value={formData.arr_time}
                        onChange={(e) => setFormData({ ...formData, arr_time: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        required
                      />
                    </div>
                    <div>
                      <label className='block test-sm font-semibold text-gray-700 mb-1'>
                        Arrival Time UTC*
                      </label>
                      <input
                        type='datetime-local'
                        value={formData.arr_time_utc}
                        onChange={(e) => setFormData({ ...formData, arr_time_utc:e.target.value })}
                        className='w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500'
                        required
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-1">
                        CS Airline IATA
                      </label>
                      <input
                        type="text"
                        value={formData.cs_airline_iata || ''}
                        onChange={(e) => setFormData({ ...formData, cs_airline_iata: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-1">
                        CS Flight Number
                      </label>
                      <input
                        type="text"
                        value={formData.cs_flight_number || ''}
                        onChange={(e) => setFormData({ ...formData, cs_flight_number: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-1">
                        CS Flight IATA
                      </label>
                      <input
                        type="text"
                        value={formData.cs_flight_iata || ''}
                        onChange={(e) => setFormData({ ...formData, cs_flight_iata: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-1">
                        Status
                      </label>
                      <select
                        value={formData.status}
                        onChange={(e) => setFormData({...formData, status:e.target.value})}
                        className='w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500'
                        required
                        >
                          <option value='scheduled'>Scheduled</option>
                          <option value='active'>Active</option>
                          <option value='landed'>Landed</option>
                        </select>
                    </div>
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-1">
                        Duration (minutes) *
                      </label>
                      <input
                        type="number"
                        value={formData.duration}
                        onChange={(e) => setFormData({ ...formData, duration: parseInt(e.target.value) || 0 })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        required
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-1">
                        Delayed
                      </label>
                      <input
                        type="number"
                        value={formData.delayed || ""}
                        onChange={(e) => setFormData({ ...formData, delayed: e.target.value ? parseInt(e.target.value) : null })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-1">
                        Departure Delayed
                      </label>
                      <input
                        type="number"
                        value={formData.dep_delayed || ""}
                        onChange={(e) => setFormData({ ...formData, dep_delayed: e.target.value ? parseInt(e.target.value) : null })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-1">
                        Arrival Delayed
                      </label>
                      <input
                        type="number"
                        value={formData.arr_delayed || ""}
                        onChange={(e) => setFormData({ ...formData, arr_delayed: e.target.value ? parseInt(e.target.value) : null })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-1">
                        Aircraft ICAO
                      </label>
                      <input
                        type="text"
                        value={formData.aircraft_icao || ''}
                        onChange={(e) => setFormData({ ...formData, aircraft_icao: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-1">
                        Arrival Time TS
                      </label>
                      <input
                        type="number"
                        value={formData.arr_time_ts}
                        onChange={(e) => setFormData({ ...formData, arr_time_ts: parseInt(e.target.value) })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-1">
                        Departure Time TS
                      </label>
                      <input
                        type="number"
                        value={formData.dep_time_ts}
                        onChange={(e) => setFormData({ ...formData, dep_time_ts: parseInt(e.target.value) })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                      />
                    </div>
                  </div>
                  <div className="flex gap-3 mt-6">
                    <button
                      type="submit"
                      className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors font-semibold"
                    >
                      {modalMode === 'add' ? 'Add Flight' : 'Update Flight'}
                    </button>
                    <button
                      type="button"
                      onClick={() => setIsModalOpen(false)}
                      className="flex-1 px-4 py-2 bg-gray-400 text-white rounded-md hover:bg-gray-500 transition-colors font-semibold"
                    >
                      Cancel
                    </button>
                  </div>
                </form>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Thống kê flights */}
      <div className="mt-6 grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-white p-4 rounded-lg shadow-md border-l-4 border-blue-500">
          <p className="text-sm text-gray-600 font-semibold">Total Flights</p>
          <p className="text-2xl font-bold text-gray-800 mt-1">{flights.length}</p>
        </div>
        <div className="bg-white p-4 rounded-lg shadow-md border-l-4 border-green-500">
          <p className="text-sm text-gray-600 font-semibold">Active</p>
          <p className="text-2xl font-bold text-green-600 mt-1">{flights.filter(f => f.status === 'Active').length}</p>
        </div>
        <div className="bg-white p-4 rounded-lg shadow-md border-l-4 border-yellow-500">
          <p className="text-sm text-gray-600 font-semibold">Scheduled</p>
          <p className="text-2xl font-bold text-yellow-600 mt-1">{flights.filter(f => f.status === 'Scheduled').length}</p>
        </div>
        <div className="bg-white p-4 rounded-lg shadow-md border-l-4 border-red-500">
          <p className="text-sm text-gray-600 font-semibold">Landed</p>
          <p className="text-2xl font-bold text-red-600 mt-1">{flights.filter(f => f.status === 'Landed').length}</p>
        </div>
      </div>
    </div>
  );
};

export default FlightsInfo;