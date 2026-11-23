-- HelpSLA: schema base para MySQL 8.
-- Modelo-alvo completo. O MVP usa os cadastros com uma empresa, uma prestadora e serviço TI.
-- Ordem de serviço e pagamento estão modelados para evolução, mas não devem ser usados no MVP.

CREATE DATABASE IF NOT EXISTS helpsla
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE helpsla;

CREATE TABLE empresa (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  razao_social VARCHAR(160) NOT NULL,
  documento VARCHAR(20) NOT NULL UNIQUE,
  email_contato VARCHAR(160),
  telefone VARCHAR(30),
  ativa BOOLEAN NOT NULL DEFAULT TRUE,
  criada_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE prestadora (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  razao_social VARCHAR(160) NOT NULL,
  documento VARCHAR(20) NOT NULL UNIQUE,
  email_contato VARCHAR(160),
  ativa BOOLEAN NOT NULL DEFAULT TRUE,
  criada_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE usuario (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  empresa_id BIGINT NOT NULL,
  prestadora_id BIGINT NULL,
  nome VARCHAR(120) NOT NULL,
  email VARCHAR(160) NOT NULL,
  senha_hash VARCHAR(255) NOT NULL,
  perfil ENUM('SOLICITANTE','TECNICO','ADMIN') NOT NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_usuario_email_empresa (empresa_id, email),
  CONSTRAINT fk_usuario_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
  CONSTRAINT fk_usuario_prestadora FOREIGN KEY (prestadora_id) REFERENCES prestadora(id)
);

CREATE TABLE servico (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  nome VARCHAR(100) NOT NULL UNIQUE,
  descricao VARCHAR(500),
  ativo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE categoria (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  servico_id BIGINT NOT NULL,
  nome VARCHAR(100) NOT NULL,
  descricao VARCHAR(500),
  ativa BOOLEAN NOT NULL DEFAULT TRUE,
  UNIQUE KEY uk_categoria_servico_nome (servico_id, nome),
  CONSTRAINT fk_categoria_servico FOREIGN KEY (servico_id) REFERENCES servico(id)
);

CREATE TABLE prestadora_servico (
  prestadora_id BIGINT NOT NULL,
  servico_id BIGINT NOT NULL,
  PRIMARY KEY (prestadora_id, servico_id),
  CONSTRAINT fk_ps_prestadora FOREIGN KEY (prestadora_id) REFERENCES prestadora(id),
  CONSTRAINT fk_ps_servico FOREIGN KEY (servico_id) REFERENCES servico(id)
);

CREATE TABLE regra_sla (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  empresa_id BIGINT NOT NULL,
  servico_id BIGINT NOT NULL,
  prioridade ENUM('BAIXA','MEDIA','ALTA','CRITICA') NOT NULL,
  prazo_minutos INT UNSIGNED NOT NULL,
  prazo_aceite_minutos INT UNSIGNED NOT NULL,
  alerta_minutos INT UNSIGNED NULL,
  ativa BOOLEAN NOT NULL DEFAULT TRUE,
  vigente_desde DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  vigente_ate DATETIME(6) NULL,
  criada_por BIGINT NOT NULL,
  criada_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_sla_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
  CONSTRAINT fk_sla_servico FOREIGN KEY (servico_id) REFERENCES servico(id),
  CONSTRAINT fk_sla_autor FOREIGN KEY (criada_por) REFERENCES usuario(id),
  CONSTRAINT ck_sla_prazo_positivo CHECK (prazo_minutos > 0),
  INDEX ix_sla_busca (empresa_id, servico_id, prioridade, ativa)
);

CREATE TABLE chamado (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  codigo VARCHAR(24) NOT NULL UNIQUE,
  empresa_id BIGINT NOT NULL,
  prestadora_id BIGINT NULL,
  solicitante_id BIGINT NOT NULL,
  tecnico_id BIGINT NULL,
  categoria_id BIGINT NOT NULL,
  regra_sla_id BIGINT NOT NULL,
  titulo VARCHAR(160) NOT NULL,
  descricao TEXT NOT NULL,
  solucao TEXT NULL,
  prioridade ENUM('BAIXA','MEDIA','ALTA','CRITICA') NOT NULL,
  status ENUM('ABERTO','EM_TRIAGEM','EM_ATENDIMENTO','AGUARDANDO_SOLICITANTE','RESOLVIDO','ENCERRADO','CANCELADO') NOT NULL DEFAULT 'ABERTO',
  aberto_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  prazo_sla DATETIME(6) NOT NULL,
  prazo_resolucao_minutos INT UNSIGNED NOT NULL,
  prazo_resolucao_sla DATETIME(6) NULL,
  assumido_em DATETIME(6) NULL,
  resolvido_em DATETIME(6) NULL,
  encerrado_em DATETIME(6) NULL,
  atualizado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_chamado_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
  CONSTRAINT fk_chamado_prestadora FOREIGN KEY (prestadora_id) REFERENCES prestadora(id),
  CONSTRAINT fk_chamado_solicitante FOREIGN KEY (solicitante_id) REFERENCES usuario(id),
  CONSTRAINT fk_chamado_tecnico FOREIGN KEY (tecnico_id) REFERENCES usuario(id),
  CONSTRAINT fk_chamado_categoria FOREIGN KEY (categoria_id) REFERENCES categoria(id),
  CONSTRAINT fk_chamado_sla FOREIGN KEY (regra_sla_id) REFERENCES regra_sla(id),
  INDEX ix_chamado_fila (prestadora_id, status, prioridade, prazo_sla),
  INDEX ix_chamado_solicitante (solicitante_id, aberto_em),
  INDEX ix_chamado_prazo (status, prazo_sla)
);

CREATE TABLE historico_chamado (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  chamado_id BIGINT NOT NULL,
  ator_id BIGINT NOT NULL,
  tipo_acao VARCHAR(40) NOT NULL,
  status_anterior ENUM('ABERTO','EM_TRIAGEM','EM_ATENDIMENTO','AGUARDANDO_SOLICITANTE','RESOLVIDO','ENCERRADO','CANCELADO') NULL,
  status_novo ENUM('ABERTO','EM_TRIAGEM','EM_ATENDIMENTO','AGUARDANDO_SOLICITANTE','RESOLVIDO','ENCERRADO','CANCELADO') NULL,
  observacao VARCHAR(1000),
  criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_historico_chamado FOREIGN KEY (chamado_id) REFERENCES chamado(id),
  CONSTRAINT fk_historico_ator FOREIGN KEY (ator_id) REFERENCES usuario(id),
  INDEX ix_historico_chamado_data (chamado_id, criado_em)
);

CREATE TABLE mensagem_chamado (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  chamado_id BIGINT NOT NULL,
  autor_id BIGINT NOT NULL,
  mensagem VARCHAR(2000) NOT NULL,
  criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_mensagem_chamado FOREIGN KEY (chamado_id) REFERENCES chamado(id),
  CONSTRAINT fk_mensagem_autor FOREIGN KEY (autor_id) REFERENCES usuario(id),
  INDEX ix_mensagem_chamado_data (chamado_id, criado_em, id)
);

CREATE TABLE avaliacao (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  chamado_id BIGINT NOT NULL UNIQUE,
  solicitante_id BIGINT NOT NULL,
  nota TINYINT UNSIGNED NOT NULL,
  comentario VARCHAR(1000),
  criada_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_avaliacao_chamado FOREIGN KEY (chamado_id) REFERENCES chamado(id),
  CONSTRAINT fk_avaliacao_solicitante FOREIGN KEY (solicitante_id) REFERENCES usuario(id),
  CONSTRAINT ck_avaliacao_nota CHECK (nota BETWEEN 1 AND 5)
);

-- Evolução futura: não criar fluxo de tela/serviço financeiro no MVP.
CREATE TABLE ordem_servico (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  chamado_id BIGINT NOT NULL UNIQUE,
  valor_total DECIMAL(12,2) NOT NULL,
  moeda CHAR(3) NOT NULL DEFAULT 'BRL',
  status ENUM('ORCAMENTO','AGUARDANDO_ACEITE','ACEITA','RECUSADA','CONCLUIDA','CANCELADA') NOT NULL,
  validade_ate DATETIME(6),
  aceita_em DATETIME(6),
  criada_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_ordem_chamado FOREIGN KEY (chamado_id) REFERENCES chamado(id),
  CONSTRAINT ck_ordem_valor CHECK (valor_total >= 0)
);

CREATE TABLE pagamento (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  ordem_servico_id BIGINT NOT NULL,
  valor DECIMAL(12,2) NOT NULL,
  moeda CHAR(3) NOT NULL DEFAULT 'BRL',
  status ENUM('PENDENTE','AUTORIZADO','PAGO','RECUSADO','ESTORNADO','CANCELADO') NOT NULL DEFAULT 'PENDENTE',
  provedor VARCHAR(60) NOT NULL,
  referencia_provedor VARCHAR(160),
  criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  atualizado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_pagamento_ordem FOREIGN KEY (ordem_servico_id) REFERENCES ordem_servico(id),
  CONSTRAINT ck_pagamento_valor CHECK (valor >= 0),
  INDEX ix_pagamento_status (status, criado_em)
);

-- Não persistir dados de cartão. Guardar apenas referências/token do provedor de pagamento.
