CREATE TABLE IF NOT EXISTS korisnici (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  imejl TEXT UNIQUE NOT NULL,
  lozinka_hes TEXT NOT NULL,
  imejl_potvrdjen BOOLEAN NOT NULL DEFAULT FALSE,
  verifikacioni_token TEXT,
  pretplata_plan TEXT NOT NULL DEFAULT 'besplatni', -- 'besplatni' | 'pro'
  kreiran_u TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS oglasi (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  izvor TEXT NOT NULL,                  -- 'KP' | 'halo-oglasi' | '4zida'
  spoljni_id TEXT NOT NULL,             -- Canonical ID sa portala
  vrsta_ponude TEXT NOT NULL DEFAULT 'prodaja', -- 'prodaja' | 'izdavanje'
  tip_nekretnine TEXT NOT NULL,         -- 'stan' | 'kuca' | 'vikendica' | 'zemljiste'
  naslov TEXT NOT NULL,
  opis TEXT,
  cena_eur NUMERIC(12, 2),
  kvadratura_m2 NUMERIC(8, 2),
  cena_po_m2 NUMERIC(10, 2),
  broj_soba NUMERIC(3, 1),
  sprat TEXT,
  ukupno_spratova TEXT,
  grejanje TEXT,
  uknjizeno BOOLEAN,
  oglasivac TEXT,                       -- 'vlasnik' | 'agencija' | 'investitor'
  grad TEXT,
  opstina TEXT,
  mikrolokacija TEXT,
  mesto_sirovo TEXT,
  link_oglasa TEXT NOT NULL,
  link_fotografije TEXT,
  sve_fotografije JSONB DEFAULT '[]'::jsonb,
  kp_obnovljen BOOLEAN DEFAULT FALSE,
  otisak_nekretnine TEXT,               -- Hash za grupisanje istog stana sa različitih portala
  objavljen_u TIMESTAMPTZ,
  prvi_put_vidjen TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  poslednji_put_vidjen TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  aktivan BOOLEAN NOT NULL DEFAULT TRUE,
  UNIQUE (izvor, spoljni_id)
);

CREATE INDEX IF NOT EXISTS idx_oglasi_pretraga
  ON oglasi (vrsta_ponude, tip_nekretnine, cena_eur, kvadratura_m2, objavljen_u DESC);
CREATE INDEX IF NOT EXISTS idx_oglasi_otisak ON oglasi (otisak_nekretnine);

CREATE TABLE IF NOT EXISTS istorija_cena (
  id BIGSERIAL PRIMARY KEY,
  oglas_id UUID NOT NULL REFERENCES oglasi(id) ON DELETE CASCADE,
  stara_cena_eur NUMERIC(12, 2),
  nova_cena_eur NUMERIC(12, 2) NOT NULL,
  zabelezeno_u TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS sacuvani_oglasi (
  korisnik_id UUID NOT NULL REFERENCES korisnici(id) ON DELETE CASCADE,
  oglas_id UUID NOT NULL REFERENCES oglasi(id) ON DELETE CASCADE,
  beleska TEXT,
  status TEXT NOT NULL DEFAULT 'sacuvano', -- 'sacuvano' | 'pozvano' | 'obidjeno' | 'sakriveno'
  sacuvano_u TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (korisnik_id, oglas_id)
);

CREATE TABLE IF NOT EXISTS alarmi_pretrage (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  korisnik_id UUID NOT NULL REFERENCES korisnici(id) ON DELETE CASCADE,
  naziv TEXT NOT NULL,
  kriterijumi JSONB NOT NULL,           -- {:tip_nekretnine "stan" :cena_od 50000 ...}
  ucestalost TEXT NOT NULL DEFAULT 'dnevno', -- 'dnevno' | 'tri_puta_nedeljno' | 'nedeljno' | 'odmah' (PRO)
  aktivan BOOLEAN NOT NULL DEFAULT TRUE,
  poslednje_slanje TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  kreiran_u TIMESTAMPTZ NOT NULL DEFAULT NOW()
);