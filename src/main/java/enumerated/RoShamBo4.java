package enumerated;

public enum RoShamBo4 implements Competitor<RoShamBo4> {
    PAPER {
        @Override
        public Outcome compete(RoShamBo4 it) {
            return compete(ROCK, it);
        }
    },
    SCISSORS {
        @Override
        public Outcome compete(RoShamBo4 it) {
            return compete(PAPER, it);
        }
    },
    ROCK {
        @Override
        public Outcome compete(RoShamBo4 it) {
            return compete(SCISSORS, it);
        }
    };

    Outcome compete(RoShamBo4 loser, RoShamBo4 opponent) {
        return (opponent == this) ? Outcome.DRAW :
                (opponent == loser) ? Outcome.WIN :
                        Outcome.LOSE;
    }

    public static void main(String[] args) {
        RoShamBo.play(RoShamBo4.class, 20);
    }
}