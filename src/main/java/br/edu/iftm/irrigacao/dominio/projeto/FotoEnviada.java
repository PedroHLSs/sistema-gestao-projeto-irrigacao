package br.edu.iftm.irrigacao.dominio.projeto;

/**
 * Foto que chegou no envio do levantamento (RF03), já lida da requisição. O nome original só
 * aparece nas mensagens de erro; o arquivo é gravado com um nome gerado pelo sistema.
 *
 * @param nomeOriginal nome do arquivo no aparelho do usuário
 * @param conteudo     os bytes do arquivo
 */
public record FotoEnviada(String nomeOriginal, byte[] conteudo) {
}
