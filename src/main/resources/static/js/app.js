// Scripts globais do Sistema de Gestão de Projetos de Irrigação (JavaScript puro, sem jQuery).

document.addEventListener('DOMContentLoaded', function () {
    marcarMenuAtivo();
    pedirConfirmacao();
    filtrarSelectsDependentes();
});

// Destaca no menu o item que corresponde à página aberta. Quando mais de um item casa com o
// caminho (como /projetos e /projetos/agenda), fica o mais específico.
function marcarMenuAtivo() {
    var caminho = window.location.pathname;
    var escolhido = null;

    document.querySelectorAll('.app-navbar .nav-link').forEach(function (link) {
        if (link.getAttribute('href') === '#') {
            return;
        }

        var destino = link.pathname;
        var casa = destino === '/'
            ? caminho === '/'
            : caminho === destino || caminho.indexOf(destino + '/') === 0;

        if (casa && (escolhido === null || destino.length > escolhido.pathname.length)) {
            escolhido = link;
        }
    });

    if (escolhido !== null) {
        escolhido.classList.add('active');
        escolhido.setAttribute('aria-current', 'page');
    }
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

// <select data-filtrado-por="idDoOutroSelect"> mostra só as opções cujo data-grupo é o valor
// escolhido no outro select (por exemplo, as propriedades do produtor escolhido). Se o grupo não
// tem opção, mostra o elemento indicado em data-aviso-vazio. Sem JavaScript, todas as opções
// aparecem e o servidor confere a escolha.
function filtrarSelectsDependentes() {
    document.querySelectorAll('select[data-filtrado-por]').forEach(function (dependente) {
        var principal = document.getElementById(dependente.getAttribute('data-filtrado-por'));
        var aviso = document.getElementById(dependente.getAttribute('data-aviso-vazio'));
        if (!principal) {
            return;
        }

        function aplicar() {
            var grupo = principal.value;
            var visiveis = [];

            Array.prototype.forEach.call(dependente.options, function (opcao) {
                if (!opcao.hasAttribute('data-grupo')) {
                    return;
                }
                var mostrar = grupo !== '' && opcao.getAttribute('data-grupo') === grupo;
                opcao.hidden = !mostrar;
                opcao.disabled = !mostrar;
                if (mostrar) {
                    visiveis.push(opcao);
                } else if (opcao.selected) {
                    dependente.value = '';
                }
            });

            // Com uma opção só, ela já vem escolhida
            if (visiveis.length === 1) {
                dependente.value = visiveis[0].value;
            }
            if (aviso) {
                aviso.hidden = !(grupo !== '' && visiveis.length === 0);
            }
        }

        principal.addEventListener('change', aplicar);
        aplicar();
    });
}
