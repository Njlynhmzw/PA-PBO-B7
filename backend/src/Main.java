import api.ApiServer;
import api.DataSeeder;

import repository.MemberRepository;
import repository.ProductRepository;
import repository.TransactionRepository;

import service.MemberService;
import service.ProductService;
import service.TransactionService;


public class Main {

    public static void main(String[] args) throws Exception {


        ProductRepository     productRepository     = new ProductRepository();
        MemberRepository      memberRepository      = new MemberRepository();
        TransactionRepository transactionRepository = new TransactionRepository(productRepository, memberRepository);

        ProductService     productService     = new ProductService(productRepository);
        MemberService      memberService      = new MemberService(memberRepository);
        TransactionService transactionService = new TransactionService(transactionRepository, productRepository);

        DataSeeder.seed(productService, memberService);

        ApiServer server = new ApiServer(productService, memberService, transactionService);
        server.start();

        System.out.println("Server berjalan. Tekan ENTER untuk berhenti...");
        System.in.read();
        server.stop();
    }
}