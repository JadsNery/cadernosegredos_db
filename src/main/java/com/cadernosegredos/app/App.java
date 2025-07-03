package com.cadernosegredos.app;

import com.cadernosegredos.config.MongoConfig;
import com.cadernosegredos.config.Neo4jConfig;
import com.cadernosegredos.config.PostgresConfig;
import com.cadernosegredos.config.RedisConfig;
import com.cadernosegredos.model.Pessoa;
import com.cadernosegredos.service.PessoaService;
import com.cadernosegredos.service.RelacionamentoService;
import com.cadernosegredos.repository.MongoLogRepositoryImpl;
import com.cadernosegredos.repository.Neo4jRelationshipRepositoryImpl;
import com.cadernosegredos.repository.PostgresPessoaRepositoryImpl;
import com.cadernosegredos.repository.RedisPessoaRepositoryImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID; // Importar UUID

public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    private static Scanner scanner;
    private static PessoaService pessoaService;
    private static RelacionamentoService relacionamentoService;

    public static void main(String[] args) {
        scanner = new Scanner(System.in);

        // Inicializar configurações
        PostgresConfig.getConnection(); // Garante que a conexão com Postgres é testada/inicializada
        RedisConfig.getJedisPool();    // Garante que o pool Jedis é testado/inicializado
        Neo4jConfig.getDriver();       // Garante que o driver Neo4j é testado/inicializado
        MongoConfig.getMongoClient();  // Garante que o cliente Mongo é testado/inicializado

        // Instanciar repositórios
        PostgresPessoaRepositoryImpl postgresPessoaRepository = new PostgresPessoaRepositoryImpl();
        RedisPessoaRepositoryImpl redisPessoaRepository = new RedisPessoaRepositoryImpl();
        MongoLogRepositoryImpl mongoLogRepository = new MongoLogRepositoryImpl();
        Neo4jRelationshipRepositoryImpl neo4jRelationshipRepository = new Neo4jRelationshipRepositoryImpl();

        // Instanciar serviços com injeção de dependência
        pessoaService = new PessoaService(postgresPessoaRepository, redisPessoaRepository, mongoLogRepository);
        relacionamentoService = new RelacionamentoService(neo4jRelationshipRepository, mongoLogRepository, postgresPessoaRepository);

        logger.info("Aplicação Caderno de Segredos iniciada.");

        // Menu principal
        int opcao;
        do {
            System.out.println("\n--- Caderno de Segredos ---");
            System.out.println("1. Cadastrar Pessoa");
            System.out.println("2. Buscar Pessoa por ID");
            System.out.println("3. Buscar Pessoa por CPF");
            System.out.println("4. Listar Todas as Pessoas");
            System.out.println("5. Atualizar Pessoa");
            System.out.println("6. Deletar Pessoa");
            System.out.println("7. Estabelecer Amizade");
            System.out.println("8. Listar Amigos");
            System.out.println("9. Remover Amizade");
            System.out.println("0. Sair");
            System.out.print("Escolha uma opção: ");
            opcao = scanner.nextInt();
            scanner.nextLine(); // Consumir a nova linha

            switch (opcao) {
                case 1:
                    cadastrarPessoa();
                    break;
                case 2:
                    buscarPessoaPorId();
                    break;
                case 3:
                    buscarPessoaPorCpf();
                    break;
                case 4:
                    listarTodasPessoas();
                    break;
                case 5:
                    atualizarPessoa();
                    break;
                case 6:
                    deletarPessoa();
                    break;
                case 7:
                    estabelecerAmizade();
                    break;
                case 8:
                    listarAmigos();
                    break;
                case 9:
                    removerAmizade();
                    break;
                case 0:
                    logger.info("Saindo da aplicação.");
                    break;
                default:
                    System.out.println("Opção inválida. Tente novamente.");
            }
        } while (opcao != 0);

        // Fechar recursos
        scanner.close();
        // Os drivers e pools serão fechados via shutdown hooks ou através dos métodos close/destroy
        RedisConfig.closeJedisPool(); // Chamar explicitamente para garantir o fechamento
        Neo4jConfig.closeDriver();    // Chamar explicitamente para garantir o fechamento
        MongoConfig.closeMongoClient(); // Chamar explicitamente para garantir o fechamento
        PostgresConfig.closeConnection(); // Se você tiver um método para fechar a pool de conexões do Postgres

        logger.info("Aplicação encerrada.");
    }

    private static void cadastrarPessoa() {
        System.out.print("Nome: ");
        String nome = scanner.nextLine();
        System.out.print("Email: ");
        String email = scanner.nextLine();
        System.out.print("CPF: ");
        String cpf = scanner.nextLine();
        System.out.print("Data de Nascimento (AAAA-MM-DD): ");
        LocalDate dataNascimento = LocalDate.parse(scanner.nextLine());

        Pessoa novaPessoa = new Pessoa(nome, email, cpf, dataNascimento);
        Pessoa savedPessoa = pessoaService.savePessoa(novaPessoa);
        if (savedPessoa != null) {
            System.out.println("Pessoa cadastrada com sucesso! ID: " + savedPessoa.getId());
        } else {
            System.out.println("Falha ao cadastrar pessoa.");
        }
    }

    private static void buscarPessoaPorId() {
        System.out.print("ID da Pessoa: ");
        String idString = scanner.nextLine();
        try {
            UUID id = UUID.fromString(idString); // Converter String para UUID
            Optional<Pessoa> pessoaOptional = pessoaService.findPessoaById(id);
            pessoaOptional.ifPresentOrElse(
                pessoa -> System.out.println("Pessoa encontrada: " + pessoa),
                () -> System.out.println("Pessoa com ID " + idString + " não encontrada.")
            );
        } catch (IllegalArgumentException e) {
            System.out.println("ID inválido. Por favor, insira um UUID válido.");
        }
    }

    private static void buscarPessoaPorCpf() {
        System.out.print("CPF da Pessoa: ");
        String cpf = scanner.nextLine();
        Optional<Pessoa> pessoaOptional = pessoaService.findPessoaByCpf(cpf);
        pessoaOptional.ifPresentOrElse(
            pessoa -> System.out.println("Pessoa encontrada: " + pessoa),
            () -> System.out.println("Pessoa com CPF " + cpf + " não encontrada.")
        );
    }

    private static void listarTodasPessoas() {
        List<Pessoa> pessoas = pessoaService.findAllPessoas();
        if (pessoas.isEmpty()) {
            System.out.println("Nenhuma pessoa cadastrada.");
        } else {
            pessoas.forEach(System.out::println);
        }
    }

    private static void atualizarPessoa() {
        System.out.print("ID da Pessoa a ser atualizada: ");
        String idString = scanner.nextLine();
        try {
            UUID id = UUID.fromString(idString);
            Optional<Pessoa> pessoaOptional = pessoaService.findPessoaById(id);

            if (pessoaOptional.isPresent()) {
                Pessoa pessoa = pessoaOptional.get();
                System.out.println("Pessoa atual: " + pessoa);

                System.out.print("Novo Nome (deixe em branco para manter '" + pessoa.getNome() + "'): ");
                String novoNome = scanner.nextLine();
                if (!novoNome.isBlank()) {
                    pessoa.setNome(novoNome);
                }

                System.out.print("Novo Email (deixe em branco para manter '" + pessoa.getEmail() + "'): ");
                String novoEmail = scanner.nextLine();
                if (!novoEmail.isBlank()) {
                    pessoa.setEmail(novoEmail);
                }

                // Não permitir alteração de CPF ou Data de Nascimento por simplicidade para ID/CPF imutáveis
                // System.out.print("Novo CPF (deixe em branco para manter '" + pessoa.getCpf() + "'): ");
                // String novoCpf = scanner.nextLine();
                // if (!novoCpf.isBlank()) {
                //     pessoa.setCpf(novoCpf);
                // }

                // System.out.print("Nova Data de Nascimento (AAAA-MM-DD, deixe em branco para manter '" + pessoa.getDataNascimento() + "'): ");
                // String novaDataNascimentoStr = scanner.nextLine();
                // if (!novaDataNascimentoStr.isBlank()) {
                //     pessoa.setDataNascimento(LocalDate.parse(novaDataNascimentoStr));
                // }

                Pessoa updatedPessoa = pessoaService.updatePessoa(pessoa);
                if (updatedPessoa != null) {
                    System.out.println("Pessoa atualizada com sucesso: " + updatedPessoa);
                } else {
                    System.out.println("Falha ao atualizar pessoa.");
                }
            } else {
                System.out.println("Pessoa com ID " + idString + " não encontrada.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("ID inválido. Por favor, insira um UUID válido.");
        }
    }

    private static void deletarPessoa() {
        System.out.print("ID da Pessoa a ser deletada: ");
        String idString = scanner.nextLine();
        try {
            UUID id = UUID.fromString(idString); // Converter String para UUID
            boolean deleted = pessoaService.deletePessoa(id);
            if (deleted) {
                System.out.println("Pessoa deletada com sucesso!");
            } else {
                System.out.println("Falha ao deletar pessoa ou pessoa não encontrada.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("ID inválido. Por favor, insira um UUID válido.");
        }
    }

    private static void estabelecerAmizade() {
        System.out.print("ID da primeira pessoa: ");
        String id1String = scanner.nextLine();
        System.out.print("ID da segunda pessoa: ");
        String id2String = scanner.nextLine();

        try {
            UUID id1 = UUID.fromString(id1String);
            UUID id2 = UUID.fromString(id2String);
            relacionamentoService.estabelecerAmizade(id1, id2);
            System.out.println("Tentativa de estabelecer amizade concluída (verifique logs para status).");
        } catch (IllegalArgumentException e) {
            System.out.println("IDs inválidos. Por favor, insira UUIDs válidos.");
        }
    }

    private static void listarAmigos() {
        System.out.print("ID da pessoa para listar amigos: ");
        String idString = scanner.nextLine();
        try {
            UUID id = UUID.fromString(idString);
            List<Pessoa> amigos = relacionamentoService.listarAmigos(id);
            if (amigos.isEmpty()) {
                System.out.println("Nenhum amigo encontrado para esta pessoa.");
            } else {
                System.out.println("Amigos:");
                amigos.forEach(amigo -> System.out.println("- " + amigo.getNome() + " (ID: " + amigo.getId() + ")"));
            }
        } catch (IllegalArgumentException e) {
            System.out.println("ID inválido. Por favor, insira um UUID válido.");
        }
    }

    private static void removerAmizade() {
        System.out.print("ID da primeira pessoa da amizade a ser removida: ");
        String id1String = scanner.nextLine();
        System.out.print("ID da segunda pessoa da amizade a ser removida: ");
        String id2String = scanner.nextLine();

        try {
            UUID id1 = UUID.fromString(id1String);
            UUID id2 = UUID.fromString(id2String);
            relacionamentoService.removerAmizade(id1, id2); // Chama o novo método
            System.out.println("Tentativa de remover amizade concluída (verifique logs para status).");
        } catch (IllegalArgumentException e) {
            System.out.println("IDs inválidos. Por favor, insira UUIDs válidos.");
        }
    }
}