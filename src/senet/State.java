package senet;

import java.util.ArrayList;

public class State {
    int blackOut;
    int whiteOut;
    ArrayList<SquareType> board = new ArrayList<>();




    public int evaluation() {
        blackOut = 0;
        whiteOut = 0;
        int eval = 0 ;


        return eval;
    }
}

/*


    State :
            {
                blackOut , whiteOut ,
                Array[SquareType]
                evaluation()
            }

    Game :
        coumputerMove() , playerMove() , miniMove() , maxMove() , chance()S

    Enum : SquareType :
                        { EMPTY, BLACK , WHITE }


 */