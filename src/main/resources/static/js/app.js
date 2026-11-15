// Scripts globais do Sistema de Gestão de Projetos de Irrigação (JavaScript puro, sem jQuery).

document.addEventListener('DOMContentLoaded', function () {
    marcarMenuAtivo();
    pedirConfirmacao();
});

// Destaca no menu o item que corresponde à página aberta.
function marcarMenuAtivo() {
    var caminho = window.location.pathname;

    document.querySelectorAll('.app-navbar .nav-link').forEach(function (link) {
        if (link.getAttribute('href') === '#') {
            return;
        }

        var destino = link.pathname;
        var ativo = destino === '/' ? caminho === '/' : caminho.indexOf(destino) === 0;

        if (ativo) {
            link.classList.add('active');
            link.setAttribute('aria-current', 'page');
        }
    });
}

// Formulário com data-confirmar="pergunta" só é enviado depois que o usuário confirma.
function pedirConfirmacao() {
    document.querySelectorAll('form[data-confirmar]').forEach(function (formulario) {
        formulario.addEventListener('submit', function (evento) {
            if (!window.confirm(formulario.getAttribute('data-confirmar'))) {
                evento.preventDefault();
            }
        });
    });
}
