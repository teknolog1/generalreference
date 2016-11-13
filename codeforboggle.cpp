#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <iostream>
#include <map>

using namespace std;

// code for c++ boggle word finding..
// new map becomes a prefix tree data structure which represents the letters to print..
struct t_node
{
	char value;

	map<char,struct t_node*> childrens; // basically, where to move next

    bool wordEnd; // whatever this node qualifies for a word end.
} * rootItem = 0;

// Inserts a string as chars into tree structure
void InsertItem(const char * toInsert)
{
    // always allocate root if null.. root is empty
    if (rootItem == NULL)
    {
        struct t_node * allocNode = new t_node;
        allocNode->value = 0;
        allocNode->wordEnd = 0;

        rootItem = allocNode;
    }

    struct t_node * currentPass = rootItem;


    int sizeToCopy = strlen(toInsert);

    for (int i=0; i < sizeToCopy; i++)
    {
        // character to copy in tree
        char insC = toInsert[i];

        // if already there.. one can move to the next child
        if (currentPass != 0 && currentPass->childrens.find(insC) != currentPass->childrens.end())
        {
            currentPass = currentPass->childrens[insC];

            // ONE should record word endings..
            if (i == sizeToCopy -1)
            {
                currentPass->wordEnd = true;
            }

            // do not need to alloc.. move to child
            continue;
        }

        // allocate the new node
        struct t_node * allocNode = new t_node;
        allocNode->value = toInsert[i];
        allocNode->wordEnd = 0;


        // IMPORTANT PARENT MUST point to child node.
        currentPass->childrens[allocNode->value] = allocNode;

        currentPass = allocNode;

        // ONE should record word endings..
        if (i == sizeToCopy -1)
        {
            allocNode->wordEnd = true;
        }
    }
}

void FreeUpNode(struct t_node * parentN)
{
    for (map<char,struct t_node*>::iterator it = parentN->childrens.begin();
         it != parentN->childrens.end(); it++) {
        char toClean = it->first;
        FreeUpNode(parentN->childrens[toClean]);
    }


    delete parentN;
}

// update the point of the boggle by ref..
void GetDestinationPointFromDirection(int & leftPos, int & topPos, int direction)
{
    switch (direction)
    {
            case 1: // TODO: use constants... THIS ONE IS UP LEFT..
                leftPos--;
                topPos--;
                break;
            case 2:
                topPos--;
                break;
            case 3:
                topPos--;
                leftPos++;
                break;
            case 4:
                leftPos--;
                break;
            case 6:
                leftPos++;
                break;
            case 7:
                leftPos--;
                topPos++;
                break;
            case 8:
                topPos++;
                break;
            case 9:
                leftPos++;
                topPos++;
                break;
    }
}



// Printout the current boggle game.
void PrintOutBoard(char** boggle, int left, int top)
{
    int sLeft, sTop;

    for (sLeft = 0; sLeft<left;sLeft++)
    {
        for (sTop=0; sTop<top; sTop++)
        {
            cout << boggle[sLeft][sTop] << "|";
        }

        cout << endl;
    }

}


/***

1 2 3

4   6

7 8 9

directional keypad for word printing..

*/

void PrintoutWord(char** boggleGame, char** bogglePath,
                    int left, int top,
                    struct t_node* wordPrefixTree,
                    int startLeft, int startTop){

    string accRun = "";


    int lPrint = startLeft;
    int bPrint = startTop;

    do
    {
        accRun += boggleGame[lPrint][bPrint];


        // stop here and print what I have... IMPORTANT
        // ONLY PRINT if in dictionary
        if (bogglePath[lPrint][bPrint] == 0)
        {
            // if what is found until now constitutes a word
            // in the word dictionary printout..
            if(wordPrefixTree->wordEnd)
            {
                cout << accRun << endl;
                return;
            }else
            {
                // okay done.. not in dictionary
                return;
            }
        }

        GetDestinationPointFromDirection(lPrint, bPrint, bogglePath[lPrint][bPrint]);
    }
    while (
           0 <= lPrint && 0 <= bPrint &&
           lPrint < left && bPrint < top
           );
}


// idea is to have a path in the BOGGLE piece..
// It is true that previously computed paths could be stored to avoid recomputation
void NextRuns(char** boggleGame, int left, int top, struct t_node* wordPrefixTree, char** map,
               int currentLeft, int currentTop,
               int startLeft, int startTop)
{
    // print current cube word
    PrintoutWord(boggleGame, map, left, top, wordPrefixTree, startLeft, startTop);


    // iterate over the navigation values.. the same method for navigation is used to update the position
    // as to where to move next..
    // why this? Actually, I only use this for the map..
    /***
        1 2 3
        4   6
        7 8 9*/
    for (int i=1; i<=9; i++)
    {
        if (i == 5) continue;

        int newLeft = currentLeft;
        int newTop = currentTop;

        // get coords for the new point
        GetDestinationPointFromDirection(newLeft, newTop, i);

        // should definately skip out of bounds
        if (
            newLeft >= left || newTop >= top ||
            newLeft  < 0  || newTop < 0
            )
            continue;


        // if blocked.. no need to reassign.. just skip also VERY IMPORTANT>
        if (map[newLeft][newTop] != 0)
            continue;

        // letter not really in wordmap..
        if (wordPrefixTree->childrens.find(boggleGame[newLeft][newTop]) == wordPrefixTree->childrens.end())
        {
            continue;
        }


        // record the directional navigation...
        // forms a path to printout the word at the end..
        map[currentLeft][currentTop] = i;

        // also update the next start point...
        NextRuns(boggleGame, left, top, wordPrefixTree->childrens[boggleGame[newLeft][newTop]] , map, newLeft, newTop, startLeft, startTop);

        // okay done, undo...
        map[currentLeft][currentTop] = 0;
    }
}


// boggle initial phase..
void InitialRun(char** boggleGame, int left, int top, struct t_node* wordPrefixTree)
{
    // stop conditions.. could be more.
    if (boggleGame == 0 || left <= 0 || top <= 0)
        return;


    // boogle path init..
    char** boggleMap = (char**)malloc(sizeof(char *) * left);

    int secAlloc = 0;
    for (secAlloc=0; secAlloc < top;secAlloc++)
    {
        boggleMap[secAlloc] = (char*) malloc(sizeof(char) * top);
        memset(boggleMap[secAlloc],0,top);
    }


    int sLeft, sTop;

    for (sLeft = 0; sLeft<left;sLeft++)
    {
        for (sTop=0; sTop<top; sTop++)
        {
            // letter not really in wordmap
            if (wordPrefixTree->childrens.find(boggleGame[sLeft][sTop]) == wordPrefixTree->childrens.end()) continue;

            // what this means.. pick the cube next location
            NextRuns(boggleGame, left, top, wordPrefixTree->childrens[boggleGame[sLeft][sTop]], boggleMap, sLeft, sTop, sLeft, sTop);

            // here the boggle map is already cleaned...
        }
    }


    // boggle map cleanup
    secAlloc = 0;
    for (secAlloc=0; secAlloc < top;secAlloc++)
    {
        free(boggleMap[secAlloc]);
    }
    free(boggleMap);
}






// 2 PTS: top is actually ->...
// actually this boggle is case sensitive..
int main()
{
    InsertItem("abc");



    char ** boggleFUN = (char**)malloc(sizeof(char *) * 3); // possible improvement: check if allocation failed.

    int secAlloc = 0;
    for (secAlloc=0; secAlloc < 3;secAlloc++)
    {
        boggleFUN[secAlloc] = (char*) malloc(sizeof(char) * 4);
        memset(boggleFUN[secAlloc],0,4);
    }

    boggleFUN[0][0] = 'a';
    boggleFUN[1][1] = 'b';
    boggleFUN[2][2] = 'c';

    boggleFUN[0][1] = 'd';
    boggleFUN[0][2] = 'e';
    boggleFUN[1][0] = 'f';
    boggleFUN[1][2] = 'g';
    boggleFUN[2][0] = 'h';
    boggleFUN[2][1] = 'y';


    boggleFUN[0][3] = ';';
    boggleFUN[1][3] = ';';
    boggleFUN[2][3] = ';';


    // STEP 3 PRINT BOGGLE cube..
    PrintOutBoard(boggleFUN, 3,4);
    // STEP 4 run this..
    InitialRun(boggleFUN, 3, 4, rootItem);


    // clean up the tree also...
    FreeUpNode(rootItem);

    // boggle cleanup
    secAlloc = 0;
    for (secAlloc=0; secAlloc < 3;secAlloc++)
    {
        free(boggleFUN[secAlloc]);
    }
    free(boggleFUN);


    return 0;
}

