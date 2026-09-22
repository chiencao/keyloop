-- Unified Service Scheduler — initial reference data (seed on startup).
-- Deterministic explicit ids keep cURL examples, the UI and tests stable.

-- Dealerships. Real lat/long near a common "home" so the UI computes true
-- haversine distance; skills vary so "doesn't offer this service" is demonstrable.
INSERT INTO dealership (id, name, opening_time, closing_time, address, rating, latitude, longitude, usual) VALUES
    (1, 'Downtown Auto Service',     '08:00:00', '18:00:00', '42 Market St', 4.8, 40.7480, -74.0020, TRUE),
    (2, 'Airport Motors',            '09:00:00', '17:00:00', '220 Skyway',   4.2, 40.7690, -73.9050, FALSE),
    (3, 'Riverside Garage',          '08:00:00', '17:00:00', '8 River Rd',   4.5, 40.6975, -73.9660, FALSE),
    (4, 'Northgate Service Center',  '07:00:00', '19:00:00', '5 North Ave',  4.6, 40.8210, -73.9550, FALSE);

-- Services. required_skill must be held by a technician; duration drives the slot length.
INSERT INTO service_type (id, code, name, duration_minutes, required_skill, price, description) VALUES
    (1, 'OIL_CHANGE',    'Oil Change',         30,  'GENERAL',     49,  'Oil & filter replacement'),
    (2, 'BRAKE_SERVICE', 'Brake Service',      60,  'BRAKES',      180, 'Pads, discs & fluid check'),
    (3, 'DIAGNOSTIC',    'Full Diagnostic',    90,  'DIAGNOSTICS', 120, 'Computer & road-test check'),
    (4, 'EV_SERVICE',    'EV Battery Service', 120, 'EV',          260, 'Battery health & software'),
    (5, 'TIRE_ROTATION', 'Tire Rotation',      30,  'GENERAL',     40,  'Rotate & balance all four'),
    (6, 'AC_SERVICE',    'A/C Service',        45,  'GENERAL',     95,  'Recharge & leak check');

INSERT INTO customer (id, name, email, phone) VALUES
    (1, 'Alice Johnson', 'alice@example.com', '+1-555-0100'),
    (2, 'Bob Smith',     'bob@example.com',   '+1-555-0101'),
    (3, 'Carla Nguyen',  'carla@example.com', '+1-555-0102'),
    (4, 'David Okafor',  'david@example.com', '+1-555-0103');

INSERT INTO vehicle (id, customer_id, vin, make, model, model_year) VALUES
    (1,  1, '1HGCM82633A004352', 'Toyota',     'Corolla',  2020),
    (2,  2, '5YJ3E1EA7KF317000', 'Tesla',      'Model 3',  2022),
    (3,  1, 'JH4KA8260MC000003', 'Honda',      'Civic',    2019),
    (4,  2, 'WBA3A5C50DF000004', 'BMW',        '320i',     2021),
    (5,  3, '1FTFW1ET5DF000005', 'Ford',       'F-150',    2018),
    (6,  3, '3VWLL7AJ9BM000006', 'Volkswagen', 'Jetta',    2020),
    (7,  4, 'KM8J3CA46JU000007', 'Hyundai',    'Tucson',   2022),
    (8,  4, '5YJSA1E26HF000008', 'Tesla',      'Model S',  2023),
    (9,  1, '2C3CDXBG5EH000009', 'Dodge',      'Charger',  2017),
    (10, 2, 'JN1AZ4EH2FM000010', 'Nissan',     '370Z',     2016);

-- Technicians and their competencies (matched to ServiceType.required_skill).
-- Coverage is intentionally uneven: only some shops can do DIAGNOSTICS / EV.
INSERT INTO technician (id, dealership_id, name) VALUES
    (1, 1, 'Carlos Mendez'),
    (2, 1, 'Dana Whitfield'),
    (3, 1, 'Evan Park'),
    (4, 2, 'Fiona Clarke'),
    (5, 3, 'Priya Shah'),
    (6, 3, 'Sam Turner'),
    (7, 4, 'Hank Rivera'),
    (8, 4, 'Iris Kim'),
    (9, 4, 'Jack Dawson');

INSERT INTO technician_skill (technician_id, skill) VALUES
    (1, 'GENERAL'), (1, 'BRAKES'),
    (2, 'GENERAL'), (2, 'DIAGNOSTICS'),
    (3, 'GENERAL'), (3, 'EV'),
    (4, 'GENERAL'),
    (5, 'GENERAL'), (5, 'BRAKES'),
    (6, 'GENERAL'),
    (7, 'GENERAL'), (7, 'BRAKES'), (7, 'DIAGNOSTICS'),
    (8, 'GENERAL'), (8, 'EV'),
    (9, 'GENERAL');

INSERT INTO service_bay (id, dealership_id, name) VALUES
    (1,  1, 'Bay A'),
    (2,  1, 'Bay B'),
    (3,  2, 'Bay C'),
    (4,  3, 'Bay 1'),
    (5,  3, 'Bay 2'),
    (6,  3, 'Bay 3'),
    (7,  4, 'Bay A'),
    (8,  4, 'Bay B'),
    (9,  4, 'Bay C'),
    (10, 4, 'Bay D');
