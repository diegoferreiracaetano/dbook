-- what a customer chooses for themselves, apart from who they are (app_user): the picture and the preferences.
-- One row per user, created on the first change; everything optional, the server's defaults apply to what is null.
CREATE TABLE user_profile (
    user_id         BIGINT PRIMARY KEY REFERENCES app_user (id),
    avatar_url      VARCHAR(500),
    language        VARCHAR(10),
    theme           VARCHAR(10),
    home_airport    VARCHAR(3),
    country         VARCHAR(2),
    currency        VARCHAR(3),
    cabin_class     VARCHAR(20),
    seat_preference VARCHAR(10),
    date_format     VARCHAR(5),
    distance_unit   VARCHAR(5)
);
