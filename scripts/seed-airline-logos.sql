-- Logos de demonstração das companhias, a partir do código IATA (CDN público de logos).
-- Só preenche quem ainda não tem logo: um logo cadastrado pelo portal não é sobrescrito.
-- Só desenvolvimento local: escreve direto no banco.
UPDATE airline
SET logo_url = 'https://pics.avs.io/200/200/' || iata_code || '.png'
WHERE logo_url IS NULL;
