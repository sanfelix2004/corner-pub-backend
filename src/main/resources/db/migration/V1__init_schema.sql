-- flyway:executeInTransaction=true

-- ===== USERS =====
CREATE TABLE IF NOT EXISTS public.users (
                                            id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                            name         VARCHAR NOT NULL,
                                            phone        VARCHAR NOT NULL UNIQUE,
                                            created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
    );

-- ===== MENU ITEMS =====
CREATE TABLE IF NOT EXISTS public.menu_items (
                                                 id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                                 categoria  VARCHAR NOT NULL,
                                                 titolo     VARCHAR NOT NULL,
                                                 descrizione TEXT,
                                                 prezzo     NUMERIC(10,2) NOT NULL CHECK (prezzo >= 0),
    imageurl   VARCHAR,
    visibile   BOOLEAN NOT NULL DEFAULT TRUE
    );

-- ===== IN EVIDENZA =====
CREATE TABLE IF NOT EXISTS public.in_evidenza (
                                                  id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                                  categoria  VARCHAR NOT NULL,
                                                  item_id    BIGINT NOT NULL REFERENCES public.menu_items(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ux_in_evidenza_item_categoria UNIQUE (item_id, categoria)
    );

-- ===== EVENTI =====
CREATE TABLE IF NOT EXISTS public.event (
                                            id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                            titolo        VARCHAR NOT NULL,
                                            descrizione   TEXT,
                                            "data"        TIMESTAMPTZ NOT NULL,          -- timestamp con timezone
                                            posti_totali  INTEGER CHECK (posti_totali IS NULL OR posti_totali >= 0),
    created_at    TIMESTAMPTZ DEFAULT now()
    );

-- ===== ISCRIZIONI EVENTO =====
CREATE TABLE IF NOT EXISTS public.event_registration (
                                                         id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                                         event_id      BIGINT REFERENCES public.event(id),
    user_id       BIGINT REFERENCES public.users(id),
    partecipanti  INTEGER NOT NULL CHECK (partecipanti > 0),
    note          VARCHAR,
    created_at    TIMESTAMPTZ DEFAULT now()
    );

-- ===== PROMOZIONI =====
CREATE TABLE IF NOT EXISTS public.promotion (
                                                id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                                nome         VARCHAR NOT NULL,
                                                descrizione  TEXT,
                                                data_inizio  DATE,
                                                data_fine    DATE,
                                                attiva       BOOLEAN DEFAULT TRUE,
                                                CONSTRAINT chk_promo_date CHECK (
                                                data_inizio IS NULL OR data_fine IS NULL OR data_inizio <= data_fine
)
    );

-- ===== PROMOZIONE <-> MENU ITEM (join) =====
CREATE TABLE IF NOT EXISTS public.promotion_menu_item (
                                                          id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                                          promotion_id        BIGINT NOT NULL REFERENCES public.promotion(id),
    menu_item_id        BIGINT NOT NULL REFERENCES public.menu_items(id),
    sconto_percentuale  NUMERIC NOT NULL CHECK (sconto_percentuale >= 0 AND sconto_percentuale <= 100),
    CONSTRAINT ux_promo_item UNIQUE (promotion_id, menu_item_id)
    );

-- ===== PRENOTAZIONI =====
CREATE TABLE IF NOT EXISTS public.reservations (
                                                   id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                                   event_id   BIGINT REFERENCES public.event(id),
    "date"     DATE NOT NULL,
    "time"     TIME WITHOUT TIME ZONE NOT NULL,
    people     INTEGER NOT NULL CHECK (people > 0),
    note       TEXT,
    user_id    BIGINT NOT NULL REFERENCES public.users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );
