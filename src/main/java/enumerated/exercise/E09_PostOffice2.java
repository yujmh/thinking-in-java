package enumerated.exercise;

import net.mindview.util.Enums;

import java.util.EnumMap;
import java.util.Iterator;

import static net.mindview.util.Print.print;

class Mail {
    // The NO's lower the probability of random selection:
    enum GeneralDelivery {
        YES, NO1, NO2, NO3, NO4, NO5
    }

    enum Scannability {UNSCANNABLE, YES1, YES2, YES3, YES4}

    enum Readability {ILLEGIBLE, YES1, YES2, YES3, YES4}

    enum Address {INCORRECT, OK1, OK2, OK3, OK4, OK5, OK6}

    enum ReturnAddress {MISSING, OK1, OK2, OK3, OK4, OK5}

    GeneralDelivery generalDelivery;

    Scannability scannability;

    Readability readability;

    Address address;

    ReturnAddress returnAddress;

    static long counter = 0;

    long id = counter++;

    @Override
    public String toString() {
        return "Mail " + id;
    }

    public String details() {
        return toString() +
                ", General Delivery: " + generalDelivery +
                ", Address Scanability: " + scannability +
                ", Address Readability: " + readability +
                ", Address Address: " + address +
                ", Return address: " + returnAddress;
    }

    // Generate test Mail
    public static Mail randomMail() {
        Mail m = new Mail();
        m.generalDelivery = Enums.random(GeneralDelivery.class);
        m.scannability = Enums.random(Scannability.class);
        m.readability = Enums.random(Readability.class);
        m.address = Enums.random(Address.class);
        m.returnAddress = Enums.random(ReturnAddress.class);
        return m;
    }

    public static Iterable<Mail> generator(final int count) {
        return new Iterable<Mail>() {
            int n = count;

            @Override
            public Iterator<Mail> iterator() {
                return new Iterator<Mail>() {
                    @Override
                    public boolean hasNext() {
                        return n-- > 0;
                    }

                    @Override
                    public Mail next() {
                        return randomMail();
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }
}

interface Command {
    boolean handle(Mail m);
}

public class E09_PostOffice2 {
    private static EnumMap<MailHandler, Command> em = new EnumMap<>(MailHandler.class);

    static {
        em.put(MailHandler.GENERAL_DELIVERY, new Command() {
            @Override
            public boolean handle(Mail m) {
                switch (m.generalDelivery) {
                    case YES:
                        print("Using general delivery for " + m);
                        return true;
                    default:
                        return false;
                }
            }
        });
        em.put(MailHandler.MACHINE_SCAN, new Command() {
            @Override
            public boolean handle(Mail m) {
                switch (m.scannability) {
                    case UNSCANNABLE:
                        return false;
                    default:
                        switch (m.address) {
                            case INCORRECT:
                                return false;
                            default:
                                print("Delivering " + m + " automatically");
                                return true;
                        }
                }
            }
        });
        em.put(MailHandler.VISUAL_INSPECTION, new Command() {
            @Override
            public boolean handle(Mail m) {
                switch (m.readability) {
                    case ILLEGIBLE:
                        return false;
                    default:
                        switch (m.address) {
                            case INCORRECT:
                                return false;
                            default:
                                print("Delivering " + m + " normally");
                                return true;
                        }
                }
            }
        });
        em.put(MailHandler.RETURN_TO_SENDER, new Command() {
            @Override
            public boolean handle(Mail m) {
                switch (m.returnAddress) {
                    case MISSING:
                        return false;
                    default:
                        print("Returning " + m + " to sender");
                        return true;
                }
            }
        });
    }

    enum MailHandler {
        GENERAL_DELIVERY, MACHINE_SCAN, VISUAL_INSPECTION, RETURN_TO_SENDER;
    }

    static void handle(Mail m) {
        for (MailHandler handler : MailHandler.values()) {
            if (em.get(handler).handle(m)) {
                return;
            }
        }
        print(m + " is a dead letter");
    }

    public static void main(String[] args) {
        for (Mail mail : Mail.generator(10)) {
            print(mail.details());
            handle(mail);
            System.out.println("*****");
        }
    }
}