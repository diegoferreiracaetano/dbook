# Tempo real (WebSocket + Redis)

[← Voltar ao README](../README.md)

Disponibilidade de assentos em tempo real, com fan-out entre instâncias.

Quando uma reserva é criada ou cancelada, a disponibilidade atualizada do `Bookable` é publicada em tempo real via WebSocket/STOMP, para clientes que estejam olhando aquela rota/voo no momento.

- Endpoint STOMP: `ws://localhost:8080/ws` (sem SockJS — o cliente é um app KMP/Compose, não uma página de navegador precisando de fallback HTTP).
- Tópico por `Bookable`: `/topic/bookables/{bookableId}/availability`, payload `{"bookableId": 1, "availableCapacity": 179}`.
- Autenticação acontece no frame STOMP `CONNECT` (header `Authorization: Bearer <accessToken>`), não no handshake HTTP — um WebSocket nativo de navegador não permite setar headers HTTP arbitrários no handshake, então `/ws` é público no `SecurityConfig` e a validação real do JWT é feita pelo `StompAuthChannelInterceptor`. Um `CONNECT` sem token válido é rejeitado com um frame `ERROR` e a conexão é fechada.
- O broadcast só acontece depois que a transação commita (`afterCommit`, ver `TransactionSupport.kt`) — uma reserva que sofre rollback nunca deveria ter avisado ninguém sobre uma mudança que não aconteceu.
- Fan-out entre instâncias é feito via Redis Pub/Sub (canal `dbook:availability`): cada instância publica no Redis ao invés de empurrar direto pras próprias sessões STOMP; toda instância também assina esse canal e reencaminha pras suas sessões locais. Com 1 instância isso parece redundante, mas é exatamente o que passa a ser necessário a partir do M6 (múltiplos containers ECS Fargate) — decisão de aprender o padrão agora, achando com 1 instância, antes de precisar dele de verdade.

Pré-requisito local: Redis também sobe pelo `docker compose up -d` (serviço `redis`, porta `6379`).
