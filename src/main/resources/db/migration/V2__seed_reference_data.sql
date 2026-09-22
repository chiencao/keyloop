-- Deterministic seed data so the API is demoable out of the box.
-- Explicit ids keep cURL examples and tests stable.

INSERT INTO dealership (id, name, opening_time, closing_time) VALUES
    (1, 'Downtown Auto Service', '08:00:00', '18:00:00'),
    (2, 'Airport Motors',        '09:00:00', '17:00:00');

INSERT INTO service_type (id, code, name, duration_minutes, required_skill) VALUES
    (1, 'OIL_CHANGE',    'Oil Change',          30,  'GENERAL'),
    (2, 'BRAKE_SERVICE', 'Brake Service',       60,  'BRAKES'),
    (3, 'DIAGNOSTIC',    'Full Diagnostic',     90,  'DIAGNOSTICS'),
    (4, 'EV_SERVICE',    'EV Battery Service',  120, 'EV');

INSERT INTO customer (id, name, email, phone) VALUES
    (1, 'Alice Johnson', 'alice@example.com', '+1-555-0100'),
    (2, 'Bob Smith',     'bob@example.com',   '+1-555-0101');

INSERT INTO vehicle (id, customer_id, vin, make, model, model_year) VALUES
    (1, 1, '1HGCM82633A004352', 'Toyota', 'Corolla',  2020),
    (2, 2, '5YJ3E1EA7KF317000', 'Tesla',  'Model 3',  2022);

-- Dealership 1 technicians and their competencies.
INSERT INTO technician (id, dealership_id, name) VALUES
    (1, 1, 'Carlos Mendez'),
    (2, 1, 'Dana Whitfield'),
    (3, 1, 'Evan Park'),
    (4, 2, 'Fiona Clarke');

INSERT INTO technician_skill (technician_id, skill) VALUES
    (1, 'GENERAL'), (1, 'BRAKES'),
    (2, 'GENERAL'), (2, 'DIAGNOSTICS'),
    (3, 'GENERAL'), (3, 'EV'),
    (4, 'GENERAL');

INSERT INTO service_bay (id, dealership_id, name) VALUES
    (1, 1, 'Bay A'),
    (2, 1, 'Bay B'),
    (3, 2, 'Bay C');
