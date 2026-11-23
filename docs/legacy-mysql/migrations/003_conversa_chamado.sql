-- Execute uma única vez em um banco helpsla já criado para habilitar a conversa nos chamados.
USE helpsla;

CREATE TABLE IF NOT EXISTS mensagem_chamado (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  chamado_id BIGINT NOT NULL,
  autor_id BIGINT NOT NULL,
  mensagem VARCHAR(2000) NOT NULL,
  criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_mensagem_chamado FOREIGN KEY (chamado_id) REFERENCES chamado(id),
  CONSTRAINT fk_mensagem_autor FOREIGN KEY (autor_id) REFERENCES usuario(id),
  INDEX ix_mensagem_chamado_data (chamado_id, criado_em, id)
);
