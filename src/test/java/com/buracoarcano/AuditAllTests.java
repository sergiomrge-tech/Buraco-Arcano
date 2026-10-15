package com.buracoarcano;

/**
 * Comando único que roda as verificações independentes por categoria.
 * Cada suíte também possui main() próprio para diagnóstico isolado.
 */
public final class AuditAllTests {
    public static void main(String[] args) {
        long start=System.currentTimeMillis();
        long cases=0;
        cases+=ThreeCardExhaustiveTest.run();
        cases+=ExtendedSequencesTest.run();
        cases+=DealAndShuffleTest.run();
        cases+=TurnAndPermissionsTest.run();
        cases+=ScoringAndCanastaTest.run();
        cases+=DeadPileAndExitTest.run();
        cases+=MeldMutationTest.run();
        cases+=BotStressTest.run();
        System.out.println("==============================================");
        System.out.println("AUDITORIA JAVA: "+cases+" casos; 0 falhas");
        System.out.println("Duração: "+((System.currentTimeMillis()-start)/1000.0)+" segundos");
        System.out.println("CSV gerado: reports/qa-trincas-104.csv");
        System.out.println("==============================================");
    }
}
