-- Additional demo customers and vehicles.
-- More vehicles let the UI show variety and let concurrency tests use a distinct
-- vehicle per booking (isolating resource-capacity limits from the
-- same-vehicle double-booking guard).

INSERT INTO customer (id, name, email, phone) VALUES
    (3, 'Carla Nguyen',  'carla@example.com',  '+1-555-0102'),
    (4, 'David Okafor',  'david@example.com',  '+1-555-0103');

INSERT INTO vehicle (id, customer_id, vin, make, model, model_year) VALUES
    (3, 1, 'JH4KA8260MC000003', 'Honda',      'Civic',    2019),
    (4, 2, 'WBA3A5C50DF000004', 'BMW',        '320i',     2021),
    (5, 3, '1FTFW1ET5DF000005', 'Ford',       'F-150',    2018),
    (6, 3, '3VWLL7AJ9BM000006', 'Volkswagen', 'Jetta',    2020),
    (7, 4, 'KM8J3CA46JU000007', 'Hyundai',    'Tucson',   2022),
    (8, 4, '5YJSA1E26HF000008', 'Tesla',      'Model S',  2023),
    (9, 1, '2C3CDXBG5EH000009', 'Dodge',      'Charger',  2017),
    (10, 2, 'JN1AZ4EH2FM000010', 'Nissan',    '370Z',     2016);
