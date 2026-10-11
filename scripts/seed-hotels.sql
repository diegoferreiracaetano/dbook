-- Hotéis de demonstração: 2 por aeroporto conhecido (um 4 estrelas, um 5), cada um com 3 tipos de quarto.
-- Idempotente: pula o hotel cujo título já existe. Só desenvolvimento local: escreve direto no banco.
-- Fotos: rotação de 6 imagens de hotel (mesmo CDN das fotos dos destinos); hotel sem foto recebe uma no fim.
-- A disponibilidade não precisa de `room_night`: sem linha, o servidor entende "nenhuma noite ocupada".
DO $$
DECLARE
  ap record;
  tier record;
  b bigint;
  hotel_title text;
BEGIN
  FOR ap IN SELECT id, iata_code, city FROM airport LOOP
    FOR tier IN
      SELECT * FROM (VALUES
        (4, 'Hotel ', ' Centro',  'wifi,breakfast,gym',          280.00),
        (5, 'Grand ', ' Resort',  'wifi,breakfast,pool,spa,gym', 520.00)
      ) AS t(stars, prefix, suffix, amenities, base_rate)
    LOOP
      hotel_title := tier.prefix || ap.city || tier.suffix;
      CONTINUE WHEN EXISTS (SELECT 1 FROM bookable WHERE bookable.title = hotel_title);

      INSERT INTO bookable (title, price, total_capacity, active, version)
      VALUES (hotel_title, 0, 1, true, 0)
      RETURNING id INTO b;

      INSERT INTO accommodation (id, destination_airport_id, address, stars, description, amenities)
      VALUES (b, ap.id, 'Centro, ' || ap.city, tier.stars,
              'Hospedagem ' || tier.stars || ' estrelas em ' || ap.city || '.', tier.amenities);

      INSERT INTO room_type (accommodation_id, name, capacity, nightly_rate, quantity, active) VALUES
        (b, 'Standard', 2, tier.base_rate,                 5, true),
        (b, 'Superior', 3, round(tier.base_rate * 1.4, 2), 3, true),
        (b, 'Suíte',    4, round(tier.base_rate * 2.2, 2), 2, true);
    END LOOP;
  END LOOP;
END $$;

UPDATE accommodation SET photo_url = (ARRAY[
  'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=600&h=400&fit=crop&auto=format',
  'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=600&h=400&fit=crop&auto=format',
  'https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=600&h=400&fit=crop&auto=format',
  'https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?w=600&h=400&fit=crop&auto=format',
  'https://images.unsplash.com/photo-1564501049412-61c2a3083791?w=600&h=400&fit=crop&auto=format',
  'https://images.unsplash.com/photo-1445019980597-93fa8acb246c?w=600&h=400&fit=crop&auto=format'
])[1 + (id % 6)::int]
WHERE photo_url IS NULL;
