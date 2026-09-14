PRAGMA foreign_keys = ON;
CREATE TABLE IF NOT EXISTS catalogo_ia (
 id TEXT PRIMARY KEY, nome TEXT NOT NULL, logo TEXT NOT NULL DEFAULT '', site TEXT NOT NULL, descricao TEXT NOT NULL DEFAULT '',
 gratuita INTEGER NOT NULL DEFAULT 0, acesso TEXT NOT NULL DEFAULT 'paga', categoria_principal TEXT, idiomas TEXT NOT NULL DEFAULT '',
 plataformas TEXT NOT NULL DEFAULT '', modelo_acesso TEXT, possui_api INTEGER, requer_login INTEGER, ultima_verificacao TEXT,
 status TEXT, origem TEXT NOT NULL, categoria_area TEXT NOT NULL, criado_em TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
 atualizado_em TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS categoria (id INTEGER PRIMARY KEY AUTOINCREMENT, chave TEXT UNIQUE NOT NULL, nome TEXT NOT NULL, area TEXT NOT NULL);
CREATE TABLE IF NOT EXISTS ia_categoria (ia_id TEXT NOT NULL REFERENCES catalogo_ia(id) ON DELETE CASCADE, categoria_id INTEGER NOT NULL REFERENCES categoria(id) ON DELETE CASCADE, PRIMARY KEY(ia_id,categoria_id));
CREATE TABLE IF NOT EXISTS capacidade (id INTEGER PRIMARY KEY AUTOINCREMENT, chave TEXT UNIQUE NOT NULL, nome TEXT NOT NULL);
CREATE TABLE IF NOT EXISTS ia_capacidade (ia_id TEXT NOT NULL REFERENCES catalogo_ia(id) ON DELETE CASCADE, capacidade_id INTEGER NOT NULL REFERENCES capacidade(id) ON DELETE CASCADE, PRIMARY KEY(ia_id,capacidade_id));
CREATE TABLE IF NOT EXISTS api (id INTEGER PRIMARY KEY AUTOINCREMENT, nome TEXT UNIQUE NOT NULL, site TEXT, docs TEXT, acesso TEXT, status TEXT);
CREATE TABLE IF NOT EXISTS modelo (id INTEGER PRIMARY KEY AUTOINCREMENT, ia_id TEXT NOT NULL REFERENCES catalogo_ia(id) ON DELETE CASCADE, nome TEXT NOT NULL, acesso TEXT, contexto TEXT);
CREATE TABLE IF NOT EXISTS fonte_descoberta (id INTEGER PRIMARY KEY AUTOINCREMENT, nome TEXT UNIQUE NOT NULL, url TEXT, tipo TEXT, confianca REAL DEFAULT 0);
CREATE TABLE IF NOT EXISTS descoberta_radar (id INTEGER PRIMARY KEY AUTOINCREMENT, externo_id TEXT UNIQUE NOT NULL, nome TEXT NOT NULL, site TEXT, area TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'EM_ANALISE', fonte TEXT, encontrado_em TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, atualizado_em TEXT);
CREATE TABLE IF NOT EXISTS alteracao_catalogo (id INTEGER PRIMARY KEY AUTOINCREMENT, ia_id TEXT, campo TEXT NOT NULL, valor_anterior TEXT, valor_novo TEXT, fonte TEXT, ocorrido_em TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE IF NOT EXISTS favorito (ia_id TEXT PRIMARY KEY REFERENCES catalogo_ia(id) ON DELETE CASCADE, criado_em TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP);
CREATE INDEX IF NOT EXISTS idx_catalogo_area ON catalogo_ia(categoria_area);
CREATE INDEX IF NOT EXISTS idx_catalogo_status ON catalogo_ia(status);
CREATE INDEX IF NOT EXISTS idx_radar_status ON descoberta_radar(status);
INSERT OR IGNORE INTO fonte_descoberta(nome,url,tipo,confianca) VALUES ('LatentBox','https://github.com/latentcat/latentbox','CATALOGO_BASE',0.95),('Explorer China','asset://explorer_china_seed.json','SEED',0.95),('Catalogo 18+','asset://ia_18_catalogo.json','SEPARADO',0.95),('Radar IaBrain','interno://radar','DESCOBERTA',0.80);
