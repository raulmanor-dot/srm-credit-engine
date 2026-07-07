INSERT INTO currencies (code, name) VALUES
    ('BRL', 'Real Brasileiro'),
    ('USD', 'Dólar Americano');

INSERT INTO receivable_types (code, name, spread_percent_monthly) VALUES
    ('DUPLICATA_MERCANTIL', 'Duplicata Mercantil', 1.5000),
    ('CHEQUE_PRE_DATADO', 'Cheque Pré-datado', 2.5000);

INSERT INTO exchange_rates (base_currency_id, quote_currency_id, rate, source)
SELECT usd.id, brl.id, 5.400000, 'MANUAL'
FROM currencies usd, currencies brl
WHERE usd.code = 'USD' AND brl.code = 'BRL';
