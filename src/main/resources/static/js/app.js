// Scripts globais do Sistema de Gestão de Projetos de Irrigação (JavaScript puro, sem jQuery).

document.addEventListener('DOMContentLoaded', function () {
    marcarMenuAtivo();
    prepararMenuLateral();
    pedirConfirmacao();
    filtrarSelectsDependentes();
    usarLocalizacaoDoAparelho();
    conferirFotosEscolhidas();
    buscarMaterial();
    imprimirPagina();
    prepararAssinatura();
});

// Botão data-imprimir: abre a janela de impressão do navegador, que também salva em PDF.
function imprimirPagina() {
    document.querySelectorAll('[data-imprimir]').forEach(function (botao) {
        botao.addEventListener('click', function () {
            window.print();
        });
    });
}

// Destaca no menu o item que corresponde à página aberta. Quando mais de um item casa com o
// caminho (como /projetos e /projetos/agenda), fica o mais específico.
function marcarMenuAtivo() {
    var caminho = window.location.pathname;
    var escolhido = null;

    document.querySelectorAll('.app-sidebar .app-menu-link').forEach(function (link) {
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

// Menu lateral (fragments/header.html). No computador, o botão do topo recolhe o menu para só os
// ícones e a escolha fica guardada neste navegador; recolhido, o nome de cada item aparece numa
// dica. No celular e no tablet, o botão da barra superior abre o menu por cima da página.
function prepararMenuLateral() {
    var menu = document.getElementById('menuLateral');
    if (!menu) {
        return;
    }
    var raiz = document.documentElement;
    var celular = window.matchMedia('(max-width: 991.98px)');
    var recolher = menu.querySelector('[data-menu-recolher]');
    var fechar = menu.querySelector('[data-menu-fechar]');
    var abrir = document.querySelector('[data-menu-abrir]');
    var fundo = document.querySelector('[data-menu-fundo]');

    // Sem o JavaScript do Bootstrap (CDN fora do ar), o menu funciona sem as dicas
    function criarDica(elemento, titulo) {
        if (typeof bootstrap === 'undefined') {
            return null;
        }
        return new bootstrap.Tooltip(elemento, { title: titulo, placement: 'right', trigger: 'hover focus' });
    }

    // Nome do item: o data-dica ou o texto escrito nele, sem o número do aviso
    function rotulo(elemento) {
        if (elemento.hasAttribute('data-dica')) {
            return elemento.getAttribute('data-dica');
        }
        return Array.prototype.filter.call(elemento.childNodes, function (no) {
            return no.nodeType === Node.TEXT_NODE;
        }).map(function (no) {
            return no.textContent.trim();
        }).join(' ').trim();
    }

    var dicasDoRecolhido = [];
    menu.querySelectorAll('.app-menu-link, .app-avatar').forEach(function (elemento) {
        var dica = criarDica(elemento, rotulo(elemento));
        if (dica) {
            dicasDoRecolhido.push(dica);
        }
    });

    // Botões só de ícone: a dica é o aria-label, lido na hora porque o do recolher muda
    var dicasDosBotoes = [];
    menu.querySelectorAll('[data-menu-recolher], [data-dica-botao]').forEach(function (botao) {
        var dica = criarDica(botao, function (elemento) {
            return elemento.getAttribute('aria-label');
        });
        if (dica) {
            dicasDosBotoes.push(dica);
        }
    });

    function aplicarRecolhido(recolhido) {
        raiz.classList.toggle('menu-recolhido', recolhido);
        if (recolher) {
            recolher.setAttribute('aria-expanded', String(!recolhido));
            recolher.setAttribute('aria-label', recolhido ? 'Expandir menu' : 'Recolher menu');
        }
        var mostrarDicas = recolhido && !celular.matches;
        dicasDoRecolhido.forEach(function (dica) {
            if (mostrarDicas) {
                dica.enable();
            } else {
                dica.hide();
                dica.disable();
            }
        });
    }

    aplicarRecolhido(raiz.classList.contains('menu-recolhido'));

    if (recolher) {
        recolher.addEventListener('click', function () {
            var recolhido = !raiz.classList.contains('menu-recolhido');
            // O botão muda de lugar: a dica aberta ficaria apontando para o lugar antigo
            dicasDosBotoes.forEach(function (dica) {
                dica.hide();
            });
            aplicarRecolhido(recolhido);
            try {
                localStorage.setItem('menuRecolhido', recolhido ? '1' : '0');
            } catch (e) {
                // Navegador sem armazenamento: a escolha vale só até trocar de página
            }
        });
    }

    if (!abrir || !fundo) {
        return;
    }

    function abrirMenu() {
        menu.classList.add('aberto');
        fundo.hidden = false;
        abrir.setAttribute('aria-expanded', 'true');
        document.body.classList.add('menu-aberto');
        var primeiro = menu.querySelector('.app-menu-link.active') || menu.querySelector('.app-menu-link');
        if (primeiro) {
            primeiro.focus();
        }
    }

    function fecharMenu(devolverFoco) {
        if (!menu.classList.contains('aberto')) {
            return;
        }
        menu.classList.remove('aberto');
        fundo.hidden = true;
        abrir.setAttribute('aria-expanded', 'false');
        document.body.classList.remove('menu-aberto');
        if (devolverFoco) {
            abrir.focus();
        }
    }

    abrir.addEventListener('click', abrirMenu);
    fundo.addEventListener('click', function () {
        fecharMenu(true);
    });
    if (fechar) {
        fechar.addEventListener('click', function () {
            fecharMenu(true);
        });
    }
    document.addEventListener('keydown', function (evento) {
        if (evento.key === 'Escape') {
            fecharMenu(true);
        }
    });

    // Girar o tablet ou aumentar a janela passa de um modo para o outro
    celular.addEventListener('change', function () {
        fecharMenu(false);
        aplicarRecolhido(raiz.classList.contains('menu-recolhido'));
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

// Botão data-localizacao: preenche latitude e longitude com o GPS do aparelho (RNF02). O navegador
// só libera a localização em HTTPS (ou em localhost); sem isso, ou com erro, a mensagem aparece e os
// campos continuam abertos para digitação.
function usarLocalizacaoDoAparelho() {
    document.querySelectorAll('[data-localizacao]').forEach(function (botao) {
        var latitude = document.getElementById(botao.getAttribute('data-latitude'));
        var longitude = document.getElementById(botao.getAttribute('data-longitude'));
        var aviso = document.getElementById(botao.getAttribute('data-aviso'));

        function avisar(texto, erro) {
            aviso.textContent = texto;
            aviso.classList.toggle('text-danger', erro);
        }

        botao.addEventListener('click', function () {
            if (!window.isSecureContext || !('geolocation' in navigator)) {
                avisar('A localização automática só funciona com HTTPS e com a localização do aparelho disponível. '
                    + 'Digite a latitude e a longitude.', true);
                return;
            }
            avisar('Buscando a sua localização...', false);
            botao.disabled = true;
            navigator.geolocation.getCurrentPosition(function (posicao) {
                latitude.value = posicao.coords.latitude.toFixed(6);
                longitude.value = posicao.coords.longitude.toFixed(6);
                avisar('Localização preenchida, com precisão de ' + Math.round(posicao.coords.accuracy) + ' m.', false);
                botao.disabled = false;
            }, function (erro) {
                var mensagens = {
                    1: 'A permissão de localização foi negada.',
                    2: 'O aparelho não conseguiu achar a posição.',
                    3: 'A localização demorou demais para responder.'
                };
                avisar((mensagens[erro.code] || 'Não foi possível obter a localização.')
                    + ' Digite a latitude e a longitude.', true);
                botao.disabled = false;
            }, { enableHighAccuracy: true, timeout: 20000, maximumAge: 0 });
        });
    });
}

// Campo de arquivo com data-limite-bytes: confere no aparelho, antes do envio, se cada foto é JPEG
// ou PNG e cabe no limite. O servidor confere de novo pelos bytes do arquivo.
function conferirFotosEscolhidas() {
    document.querySelectorAll('input[type=file][data-limite-bytes]').forEach(function (campo) {
        var limite = parseInt(campo.getAttribute('data-limite-bytes'), 10);
        var aviso = document.getElementById(campo.getAttribute('data-aviso'));
        var textoOriginal = aviso ? aviso.textContent : '';

        campo.addEventListener('change', function () {
            var problemas = [];
            var nomes = [];
            Array.prototype.forEach.call(campo.files, function (arquivo) {
                if (arquivo.type !== 'image/jpeg' && arquivo.type !== 'image/png') {
                    problemas.push(arquivo.name + ' não é JPEG nem PNG');
                } else if (arquivo.size > limite) {
                    problemas.push(arquivo.name + ' passa de 5 MB');
                } else {
                    nomes.push(arquivo.name);
                }
            });
            if (!aviso) {
                return;
            }
            if (problemas.length > 0) {
                campo.value = '';
                aviso.textContent = 'Escolha de novo: ' + problemas.join('; ') + '.';
                aviso.classList.add('text-danger');
            } else {
                aviso.textContent = nomes.length > 0
                    ? nomes.length + (nomes.length === 1 ? ' foto escolhida: ' : ' fotos escolhidas: ') + nomes.join(', ')
                    : textoOriginal;
                aviso.classList.remove('text-danger');
            }
        });
    });
}

// Campo data-busca-material: busca materiais no endpoint JSON do estoque 300 ms depois da última
// tecla e, ao escolher um, preenche os campos escondidos do formulário (id e rótulo).
function buscarMaterial() {
    document.querySelectorAll('[data-busca-material]').forEach(function (campo) {
        var endereco = campo.getAttribute('data-busca-material');
        var alvo = document.getElementById(campo.getAttribute('data-alvo'));
        var rotulo = document.getElementById(campo.getAttribute('data-rotulo'));
        var resultados = document.getElementById(campo.getAttribute('data-resultados'));
        var proximo = document.getElementById(campo.getAttribute('data-proximo'));
        var espera = null;
        var pedido = null;

        function limpar() {
            resultados.replaceChildren();
        }

        function linha(texto) {
            var item = document.createElement('div');
            item.className = 'list-group-item text-muted';
            item.textContent = texto;
            resultados.replaceChildren(item);
        }

        function numero(valor, casas) {
            return Number(valor).toLocaleString('pt-BR', { maximumFractionDigits: casas });
        }

        function mostrar(materiais) {
            if (materiais.length === 0) {
                linha('Nenhum material encontrado.');
                return;
            }
            limpar();
            materiais.forEach(function (material) {
                var opcao = document.createElement('button');
                opcao.type = 'button';
                opcao.className = 'list-group-item list-group-item-action';
                opcao.setAttribute('role', 'option');
                var titulo = document.createElement('div');
                titulo.className = 'fw-semibold';
                titulo.textContent = material.codigo + ' · ' + material.descricao;
                var detalhe = document.createElement('div');
                detalhe.className = 'small text-muted';
                detalhe.textContent = 'R$ ' + numero(material.precoVenda, 2) + ' por ' + material.unidade
                    + ' · disponível: ' + numero(material.disponivel, 3) + ' ' + material.unidade;
                opcao.append(titulo, detalhe);
                opcao.addEventListener('click', function () {
                    alvo.value = material.id;
                    rotulo.value = material.codigo + ' · ' + material.descricao;
                    campo.value = rotulo.value;
                    limpar();
                    if (proximo) {
                        proximo.focus();
                    }
                });
                resultados.appendChild(opcao);
            });
        }

        function buscar(termo) {
            if (pedido) {
                pedido.abort();
            }
            pedido = new AbortController();
            fetch(endereco + '?q=' + encodeURIComponent(termo), {
                headers: { 'Accept': 'application/json' },
                credentials: 'same-origin',
                signal: pedido.signal
            }).then(function (resposta) {
                if (!resposta.ok) {
                    throw new Error('HTTP ' + resposta.status);
                }
                return resposta.json();
            }).then(mostrar).catch(function (erro) {
                if (erro.name !== 'AbortError') {
                    linha('Não foi possível buscar os materiais agora.');
                }
            });
        }

        campo.addEventListener('input', function () {
            // Texto mudou: o material escolhido antes deixa de valer
            alvo.value = '';
            rotulo.value = '';
            clearTimeout(espera);
            var termo = campo.value.trim();
            if (termo.length < 2) {
                limpar();
                return;
            }
            espera = setTimeout(function () {
                buscar(termo);
            }, 300);
        });

        campo.addEventListener('keydown', function (evento) {
            if (evento.key === 'Escape') {
                limpar();
            }
        });
    });
}

// Quadro de assinatura do aceite (RF18): <canvas data-assinatura data-campo="idDoHidden"
// data-aviso="idDoAviso">, com um botão data-limpar-assinatura="idDoCanvas". Pointer Events
// cobrem mouse, caneta e toque. No envio, o desenho vai para o campo escondido como PNG em base64;
// quadro sem traço não envia. O servidor confere de novo.
function prepararAssinatura() {
    document.querySelectorAll('canvas[data-assinatura]').forEach(function (quadro) {
        var campo = document.getElementById(quadro.getAttribute('data-campo'));
        var aviso = document.getElementById(quadro.getAttribute('data-aviso'));
        var formulario = quadro.closest('form');
        var contexto = quadro.getContext('2d');
        var desenhando = false;
        var temTraco = false;

        // O canvas desenha na resolução da tela, para o traço não sair borrado no celular
        function ajustarTamanho() {
            var escala = window.devicePixelRatio || 1;
            var largura = quadro.clientWidth;
            var altura = quadro.clientHeight;
            quadro.width = Math.round(largura * escala);
            quadro.height = Math.round(altura * escala);
            contexto.setTransform(escala, 0, 0, escala, 0, 0);
            contexto.lineWidth = 2.5;
            contexto.lineCap = 'round';
            contexto.lineJoin = 'round';
            contexto.strokeStyle = '#1a1a1a';
            temTraco = false;
            campo.value = '';
        }

        function ponto(evento) {
            var area = quadro.getBoundingClientRect();
            return { x: evento.clientX - area.left, y: evento.clientY - area.top };
        }

        quadro.addEventListener('pointerdown', function (evento) {
            evento.preventDefault();
            quadro.setPointerCapture(evento.pointerId);
            desenhando = true;
            var p = ponto(evento);
            contexto.beginPath();
            contexto.moveTo(p.x, p.y);
            // Um toque sem arrastar também deixa marca
            contexto.lineTo(p.x + 0.1, p.y + 0.1);
            contexto.stroke();
            temTraco = true;
        });
        quadro.addEventListener('pointermove', function (evento) {
            if (!desenhando) {
                return;
            }
            evento.preventDefault();
            var p = ponto(evento);
            contexto.lineTo(p.x, p.y);
            contexto.stroke();
        });
        ['pointerup', 'pointercancel', 'pointerleave'].forEach(function (tipo) {
            quadro.addEventListener(tipo, function () {
                desenhando = false;
            });
        });

        document.querySelectorAll('[data-limpar-assinatura="' + quadro.id + '"]').forEach(function (botao) {
            botao.addEventListener('click', function () {
                contexto.clearRect(0, 0, quadro.width, quadro.height);
                temTraco = false;
                campo.value = '';
                if (aviso) {
                    aviso.textContent = '';
                }
            });
        });

        formulario.addEventListener('submit', function (evento) {
            if (!temTraco) {
                evento.preventDefault();
                if (aviso) {
                    aviso.textContent = 'Peça ao cliente para assinar no quadro antes de salvar.';
                }
                return;
            }
            campo.value = quadro.toDataURL('image/png');
        });

        ajustarTamanho();
        // Girar o celular muda a largura do quadro; o desenho recomeça para não ficar deformado
        window.addEventListener('orientationchange', ajustarTamanho);
    });
}
