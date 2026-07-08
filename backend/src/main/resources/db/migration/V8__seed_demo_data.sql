-- Massa de dados de demonstracao (frontend): cedentes, recebiveis em varios
-- status (PENDING/SETTLED/CANCELED), liquidacoes com valores calculados pela
-- mesma formula do motor de precificacao, e historico de cambio USD/BRL.
-- Serve para o Painel do Operador e a Grid de Transacoes terem volume o
-- suficiente para demonstrar paginacao, filtros dinamicos e o grafico de
-- liquidacoes por moeda no Grafana.

INSERT INTO assignors (name, tax_id) VALUES
    ('Comercial Aurora Textil Ltda', '12.345.678/0001-90'),
    ('Distribuidora Vale Verde S.A.', '23.456.789/0001-01'),
    ('Metalurgica Sao Bento Ltda', '34.567.890/0001-12'),
    ('Agroindustrial Bom Futuro S.A.', '45.678.901/0001-23'),
    ('Importadora Atlantico Ltda', '56.789.012/0001-34'),
    ('Papelaria Central do Brasil Ltda', '67.890.123/0001-45'),
    ('Construtora Horizonte S.A.', '78.901.234/0001-56'),
    ('Eletronicos Nova Era Ltda', '89.012.345/0001-67'),
    ('Cosmeticos Bela Flor Ltda', '90.123.456/0001-78'),
    ('Transportadora Rota Sul S.A.', '01.234.567/0001-89');

INSERT INTO exchange_rates (base_currency_id, quote_currency_id, rate, source, valid_from)
SELECT usd.id, brl.id, v.rate::numeric, v.source, v.valid_from::timestamptz
FROM currencies usd, currencies brl,
(VALUES
    ('2026-01-15 09:00:00+00', '5.180000', 'MANUAL'),
    ('2026-02-10 09:00:00+00', '5.225000', 'MOCK_PROVIDER'),
    ('2026-03-05 09:00:00+00', '5.310000', 'MOCK_PROVIDER'),
    ('2026-04-02 09:00:00+00', '5.260000', 'MANUAL'),
    ('2026-05-20 09:00:00+00', '5.375000', 'MOCK_PROVIDER'),
    ('2026-06-18 09:00:00+00', '5.402000', 'MOCK_PROVIDER')
) AS v(valid_from, rate, source)
WHERE usd.code = 'USD' AND brl.code = 'BRL';

INSERT INTO receivables
    (assignor_id, receivable_type_id, face_value_currency_id, face_value,
     document_number, issue_date, due_date, status)
SELECT a.id, rt.id, c.id, v.face_value, v.document_number,
       v.issue_date::date, v.due_date::date, v.status
FROM (VALUES
    (9, 'DUPLICATA_MERCANTIL', 'USD', 39171.85, 'NF-2026001', '2026-06-26', '2026-08-21', 'CANCELED'),
    (5, 'DUPLICATA_MERCANTIL', 'BRL', 234586.24, 'NF-2026002', '2026-06-01', '2026-07-30', 'PENDING'),
    (8, 'CHEQUE_PRE_DATADO', 'USD', 153936.62, 'NF-2026003', '2026-04-17', '2026-05-25', 'SETTLED'),
    (1, 'CHEQUE_PRE_DATADO', 'USD', 63593.81, 'NF-2026004', '2026-04-30', '2026-06-19', 'CANCELED'),
    (1, 'DUPLICATA_MERCANTIL', 'BRL', 92141.54, 'NF-2026005', '2026-01-03', '2026-05-03', 'SETTLED'),
    (4, 'DUPLICATA_MERCANTIL', 'BRL', 106833.02, 'NF-2026006', '2026-01-23', '2026-05-01', 'SETTLED'),
    (9, 'DUPLICATA_MERCANTIL', 'BRL', 134813.76, 'NF-2026007', '2026-01-27', '2026-04-24', 'SETTLED'),
    (5, 'DUPLICATA_MERCANTIL', 'BRL', 85677.54, 'NF-2026008', '2025-12-15', '2026-02-09', 'SETTLED'),
    (9, 'CHEQUE_PRE_DATADO', 'BRL', 238572.58, 'NF-2026009', '2026-03-23', '2026-06-15', 'SETTLED'),
    (1, 'CHEQUE_PRE_DATADO', 'BRL', 118962.78, 'NF-2026010', '2026-05-17', '2026-08-13', 'PENDING'),
    (4, 'DUPLICATA_MERCANTIL', 'BRL', 21580.9, 'NF-2026011', '2026-01-20', '2026-03-22', 'SETTLED'),
    (6, 'DUPLICATA_MERCANTIL', 'BRL', 136920.45, 'NF-2026012', '2026-03-05', '2026-06-29', 'SETTLED'),
    (10, 'CHEQUE_PRE_DATADO', 'BRL', 157175.52, 'NF-2026013', '2026-06-07', '2026-08-17', 'PENDING'),
    (3, 'DUPLICATA_MERCANTIL', 'BRL', 86276.99, 'NF-2026014', '2026-03-05', '2026-05-31', 'SETTLED'),
    (8, 'DUPLICATA_MERCANTIL', 'USD', 221857.26, 'NF-2026015', '2026-03-11', '2026-04-15', 'SETTLED'),
    (8, 'CHEQUE_PRE_DATADO', 'BRL', 192135.94, 'NF-2026016', '2026-07-07', '2026-08-21', 'PENDING'),
    (3, 'DUPLICATA_MERCANTIL', 'BRL', 64105.77, 'NF-2026017', '2026-07-05', '2026-08-27', 'PENDING'),
    (2, 'DUPLICATA_MERCANTIL', 'BRL', 205433.01, 'NF-2026018', '2026-03-06', '2026-06-23', 'SETTLED'),
    (8, 'DUPLICATA_MERCANTIL', 'USD', 82951.38, 'NF-2026019', '2026-02-11', '2026-05-02', 'SETTLED'),
    (7, 'CHEQUE_PRE_DATADO', 'BRL', 105564.31, 'NF-2026020', '2026-03-09', '2026-06-02', 'CANCELED'),
    (10, 'DUPLICATA_MERCANTIL', 'BRL', 22855.89, 'NF-2026021', '2026-01-21', '2026-04-11', 'SETTLED'),
    (9, 'CHEQUE_PRE_DATADO', 'USD', 217321.05, 'NF-2026022', '2026-06-14', '2026-08-18', 'PENDING'),
    (1, 'DUPLICATA_MERCANTIL', 'BRL', 155394.84, 'NF-2026023', '2025-12-27', '2026-03-28', 'SETTLED'),
    (1, 'CHEQUE_PRE_DATADO', 'BRL', 80285.16, 'NF-2026024', '2026-05-17', '2026-07-01', 'SETTLED'),
    (2, 'CHEQUE_PRE_DATADO', 'BRL', 44999.69, 'NF-2026025', '2026-06-04', '2026-07-15', 'CANCELED'),
    (1, 'DUPLICATA_MERCANTIL', 'USD', 188112.35, 'NF-2026026', '2026-05-28', '2026-07-31', 'PENDING'),
    (4, 'CHEQUE_PRE_DATADO', 'USD', 168118.41, 'NF-2026027', '2026-06-22', '2026-07-22', 'CANCELED'),
    (1, 'DUPLICATA_MERCANTIL', 'BRL', 48717.75, 'NF-2026028', '2026-02-05', '2026-05-08', 'SETTLED'),
    (1, 'DUPLICATA_MERCANTIL', 'BRL', 181812.16, 'NF-2026029', '2025-12-03', '2026-03-01', 'SETTLED'),
    (5, 'CHEQUE_PRE_DATADO', 'BRL', 176263.1, 'NF-2026030', '2025-12-21', '2026-04-18', 'SETTLED'),
    (8, 'DUPLICATA_MERCANTIL', 'BRL', 177706.96, 'NF-2026031', '2026-05-19', '2026-06-24', 'SETTLED'),
    (6, 'DUPLICATA_MERCANTIL', 'BRL', 52962.62, 'NF-2026032', '2026-04-11', '2026-05-26', 'SETTLED'),
    (6, 'DUPLICATA_MERCANTIL', 'BRL', 118587.46, 'NF-2026033', '2026-02-20', '2026-03-31', 'SETTLED'),
    (8, 'CHEQUE_PRE_DATADO', 'BRL', 215866.87, 'NF-2026034', '2026-05-29', '2026-09-02', 'PENDING'),
    (4, 'DUPLICATA_MERCANTIL', 'USD', 158075.97, 'NF-2026035', '2026-02-04', '2026-03-29', 'SETTLED'),
    (5, 'CHEQUE_PRE_DATADO', 'USD', 156228.06, 'NF-2026036', '2025-11-16', '2026-02-12', 'SETTLED'),
    (1, 'DUPLICATA_MERCANTIL', 'BRL', 27453.93, 'NF-2026037', '2026-02-03', '2026-03-29', 'SETTLED'),
    (6, 'CHEQUE_PRE_DATADO', 'BRL', 54398.19, 'NF-2026038', '2026-01-12', '2026-04-09', 'SETTLED'),
    (7, 'CHEQUE_PRE_DATADO', 'USD', 228374.96, 'NF-2026039', '2026-01-06', '2026-02-17', 'SETTLED'),
    (2, 'CHEQUE_PRE_DATADO', 'BRL', 214536.4, 'NF-2026040', '2026-05-09', '2026-06-30', 'SETTLED'),
    (4, 'CHEQUE_PRE_DATADO', 'USD', 84862.35, 'NF-2026041', '2026-06-13', '2026-08-10', 'PENDING'),
    (4, 'DUPLICATA_MERCANTIL', 'BRL', 68650.73, 'NF-2026042', '2025-11-15', '2026-02-22', 'SETTLED'),
    (2, 'DUPLICATA_MERCANTIL', 'BRL', 66156.84, 'NF-2026043', '2026-06-23', '2026-08-08', 'PENDING'),
    (10, 'DUPLICATA_MERCANTIL', 'BRL', 216942.63, 'NF-2026044', '2026-02-04', '2026-04-08', 'SETTLED'),
    (10, 'DUPLICATA_MERCANTIL', 'BRL', 36749.77, 'NF-2026045', '2026-04-10', '2026-07-01', 'SETTLED')
) AS v(assignor_seq, rtype_code, ccy_code, face_value, document_number, issue_date, due_date, status)
JOIN assignors a ON a.id = (SELECT id FROM assignors ORDER BY id LIMIT 1 OFFSET v.assignor_seq - 1)
JOIN receivable_types rt ON rt.code = v.rtype_code
JOIN currencies c ON c.code = v.ccy_code;

INSERT INTO settlements
    (receivable_id, payment_currency_id, face_value, face_value_currency_id,
     base_rate_used, spread_used, term_days, term_months,
     present_value_face_currency, fx_rate_used, net_value_payment_currency, settled_at)
SELECT rec.id, pay_c.id, v.face_value, face_c.id,
       v.base_rate_used, v.spread_used, v.term_days, v.term_months,
       v.present_value, v.fx_rate_used, v.net_value, v.settled_at::timestamptz
FROM (VALUES
    ('NF-2026003', 'USD', 'BRL', 153936.62, 1.5, 2.5, 5, 0.166667, 152933.650999, 5.4, 825841.715395, '2026-05-20 14:30:00+00'),
    ('NF-2026005', 'BRL', 'USD', 92141.54, 1.5, 1.5, 18, 0.6, 90521.789684, 0.185185, 16763.277623, '2026-04-15 14:30:00+00'),
    ('NF-2026006', 'BRL', 'BRL', 106833.02, 1.6, 1.5, 9, 0.3, 105859.028933, NULL, 105859.028933, '2026-04-22 14:30:00+00'),
    ('NF-2026007', 'BRL', 'BRL', 134813.76, 1.3, 1.5, 9, 0.3, 133701.502289, NULL, 133701.502289, '2026-04-15 14:30:00+00'),
    ('NF-2026008', 'BRL', 'BRL', 85677.54, 1.2, 1.5, 5, 0.166667, 85297.947529, NULL, 85297.947529, '2026-02-04 14:30:00+00'),
    ('NF-2026009', 'BRL', 'BRL', 238572.58, 1.5, 2.5, 3, 0.1, 237638.713869, NULL, 237638.713869, '2026-06-12 14:30:00+00'),
    ('NF-2026011', 'BRL', 'BRL', 21580.9, 1.1, 1.5, 6, 0.2, 21470.397263, NULL, 21470.397263, '2026-03-16 14:30:00+00'),
    ('NF-2026012', 'BRL', 'BRL', 136920.45, 1.1, 1.5, 14, 0.466667, 135290.161782, NULL, 135290.161782, '2026-06-15 14:30:00+00'),
    ('NF-2026014', 'BRL', 'BRL', 86276.99, 1.5, 1.5, 14, 0.466667, 85095.046576, NULL, 85095.046576, '2026-05-17 14:30:00+00'),
    ('NF-2026015', 'USD', 'USD', 221857.26, 1.6, 1.5, 11, 0.366667, 219387.628913, NULL, 219387.628913, '2026-04-04 14:30:00+00'),
    ('NF-2026018', 'BRL', 'BRL', 205433.01, 1, 1.5, 3, 0.1, 204926.368, NULL, 204926.368, '2026-06-20 14:30:00+00'),
    ('NF-2026019', 'USD', 'BRL', 82951.38, 1.1, 1.5, 9, 0.3, 82315.080496, 5.4, 444501.434678, '2026-04-23 14:30:00+00'),
    ('NF-2026021', 'BRL', 'BRL', 22855.89, 1.5, 1.5, 11, 0.366667, 22609.510236, NULL, 22609.510236, '2026-03-31 14:30:00+00'),
    ('NF-2026023', 'BRL', 'BRL', 155394.84, 1.5, 1.5, 17, 0.566667, 152813.656053, NULL, 152813.656053, '2026-03-11 14:30:00+00'),
    ('NF-2026024', 'BRL', 'BRL', 80285.16, 0.9, 2.5, 9, 0.3, 79483.890354, NULL, 79483.890354, '2026-06-22 14:30:00+00'),
    ('NF-2026028', 'BRL', 'BRL', 48717.75, 1.5, 1.5, 8, 0.266667, 48335.249259, NULL, 48335.249259, '2026-04-30 14:30:00+00'),
    ('NF-2026029', 'BRL', 'BRL', 181812.16, 1.2, 1.5, 11, 0.366667, 180044.736871, NULL, 180044.736871, '2026-02-18 14:30:00+00'),
    ('NF-2026030', 'BRL', 'BRL', 176263.1, 0.9, 2.5, 14, 0.466667, 173534.229769, NULL, 173534.229769, '2026-04-04 14:30:00+00'),
    ('NF-2026031', 'BRL', 'BRL', 177706.96, 1.2, 1.5, 15, 0.5, 175355.428827, NULL, 175355.428827, '2026-06-09 14:30:00+00'),
    ('NF-2026032', 'BRL', 'BRL', 52962.62, 1.5, 1.5, 5, 0.166667, 52702.343048, NULL, 52702.343048, '2026-05-21 14:30:00+00'),
    ('NF-2026033', 'BRL', 'USD', 118587.46, 1.3, 1.5, 18, 0.6, 116638.761166, 0.185185, 21599.748987, '2026-03-13 14:30:00+00'),
    ('NF-2026035', 'USD', 'USD', 158075.97, 0.9, 1.5, 19, 0.633333, 155719.338293, NULL, 155719.338293, '2026-03-10 14:30:00+00'),
    ('NF-2026036', 'USD', 'BRL', 156228.06, 1.5, 2.5, 15, 0.5, 153194.216637, 5.4, 827248.76984, '2026-01-28 14:30:00+00'),
    ('NF-2026037', 'BRL', 'BRL', 27453.93, 1.1, 1.5, 8, 0.266667, 27266.656842, NULL, 27266.656842, '2026-03-21 14:30:00+00'),
    ('NF-2026038', 'BRL', 'BRL', 54398.19, 1.6, 2.5, 20, 0.666667, 52960.323579, NULL, 52960.323579, '2026-03-20 14:30:00+00'),
    ('NF-2026039', 'USD', 'USD', 228374.96, 1.1, 2.5, 7, 0.233333, 226498.088589, NULL, 226498.088589, '2026-02-10 14:30:00+00'),
    ('NF-2026040', 'BRL', 'BRL', 214536.4, 0.9, 2.5, 13, 0.433333, 211450.51891, NULL, 211450.51891, '2026-06-17 14:30:00+00'),
    ('NF-2026042', 'BRL', 'USD', 68650.73, 1.6, 1.5, 10, 0.333333, 67955.655274, 0.185185, 12584.368022, '2026-02-12 14:30:00+00'),
    ('NF-2026044', 'BRL', 'BRL', 216942.63, 1.6, 1.5, 17, 0.566667, 213221.825455, NULL, 213221.825455, '2026-03-22 14:30:00+00'),
    ('NF-2026045', 'BRL', 'BRL', 36749.77, 1.6, 1.5, 18, 0.6, 36082.733125, NULL, 36082.733125, '2026-06-13 14:30:00+00')
) AS v(document_number, face_ccy_code, pay_ccy_code, face_value, base_rate_used,
       spread_used, term_days, term_months, present_value, fx_rate_used, net_value, settled_at)
JOIN receivables rec ON rec.document_number = v.document_number
JOIN currencies face_c ON face_c.code = v.face_ccy_code
JOIN currencies pay_c ON pay_c.code = v.pay_ccy_code;

UPDATE receivables SET version = 1, updated_at = now()
WHERE status = 'SETTLED';

