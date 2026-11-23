package model.entity;

import model.enums.Perfil;

public record Usuario(long id, long empresaId, Long prestadoraId, String nome, String email, Perfil perfil) { }
