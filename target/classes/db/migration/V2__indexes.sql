-- flyway:executeInTransaction=false

-- ===== FK & ricerca per data =====
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_event_reg_event    ON public.event_registration (event_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_event_reg_user     ON public.event_registration (user_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_reservations_user  ON public.reservations (user_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_reservations_event ON public.reservations (event_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_reservations_dt    ON public.reservations ("date","time");

-- ===== Eventi visibilità/ordinamento =====
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_event_data         ON public."event" ("data");

-- ===== Menu / evidenza =====
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_menu_items_visibile   ON public.menu_items (visibile) WHERE visibile = TRUE;
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_menu_items_categoria  ON public.menu_items (categoria);
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_in_evidenza_item      ON public.in_evidenza (item_id);

-- ===== Promozioni =====
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_promo_attiva       ON public.promotion (attiva) WHERE attiva = TRUE;
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_promo_date_inizio  ON public.promotion (data_inizio);
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_promo_date_fine    ON public.promotion (data_fine);
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_promo_item_promo   ON public.promotion_menu_item (promotion_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_promo_item_menu    ON public.promotion_menu_item (menu_item_id);
