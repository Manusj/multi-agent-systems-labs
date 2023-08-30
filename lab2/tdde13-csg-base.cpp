// --------------------------------
// TDDE13 - Multi-agent systems lab
// --------------------------------
// Base classes and input/output handling for Task 2.
// Your implementation goes inside the "solve" function.
// You should not have to do any implementations elsewhere to pass
// this part of the lab.
// Good luck!

#include <iostream>
#include <vector>
#include <math.h>
#include <algorithm>

using namespace std;

/**
* A coalition type (uses int-based bitmasks).
* While this one is very effect, you can use another representation if you like,
* for example lists, hash maps/sets, etc.
*/
class coalition_t
{
public:

  // Adds a given agent to the coalition.
  void add_agent(const int agent)
  {
    mask |= (1 << agent);
  }

  // Removes a given agent from the coalition.
  void remove_agent(const int agent)
  {
    mask &= ~(1 << agent);
  }

  // Returns true if a given agent is in the coalition.
  bool is_agent_in_coalition(const int agent) const
  {
    return (mask & (1 << agent)) != 0;
  }

  // Returns the mask representation of the coalition.
  int get_mask() const
  {
    return mask;
  }

  // Returns the number of agents in the coalition.
  int get_n_agents() const
  {
    int temp{mask};
    int res{};
    while (temp != 0)
    {
      res += temp & 1;
      temp >>= 1;
    }
    return res;
  }

private:
  int mask{};
};

/**
* A coalition structure generation (CSG) problem type.
*/
class CSG_problem_t
{
public:
  const int n;

  // Initializes a CSG problem with n agents.
  CSG_problem_t(const int _n) : coalitional_values(1 << n, 0),n(_n){}

  // Returns the value of a given coalition.
  int get_value_of_coalition(const coalition_t coalition) const
  {
    return coalitional_values[coalition.get_mask()];
  }

  void set_value_of_coalition(const coalition_t coalition, const int value)
  {
    coalitional_values[coalition.get_mask()] = value;
  }
private:
  vector<int> coalitional_values;
};

/**
* A coalition structure type.
*/
struct coalition_structure_t : vector<coalition_t>
{
};

void printTheArray(vector<int> arr, int n)
{
    for (int i = 0; i < n; i++) {
        cout << arr.at(i) << " ";
    }
    cout << endl;
}

// Function to generate all binary strings
void generateAllBinaryStrings(int n, vector<int> &arr, int i, vector<vector<int>> &result)
{
    if (i == n) {
        if(all_of(arr.begin(), arr.end(), [](int i){return i == 0;}))
          return;
        vector<int> copy = arr;
        result.push_back(copy);
        //printTheArray(arr, n);
        return;
    }

    // First assign "0" at ith position
    // and try for all other permutations
    // for remaining positions
    arr.at(i) = 0;
    generateAllBinaryStrings(n, arr, i + 1, result);

    // And then assign "1" at ith position
    // and try for all other permutations
    // for remaining positions
    arr.at(i) = 1;
    generateAllBinaryStrings(n, arr, i + 1, result);
}

/**
* Solves a given CSG problem instance.
*/
coalition_structure_t solve(const CSG_problem_t & problem)
{
  coalition_structure_t result;
  coalition_structure_t grand_coalitionresult;
  // ------------------------------------------------------------
  // Your implementation goes here ...
  // The code below just forms the grand coalition and returns it.
  // You can submit it to Kattis to see how (bad) it performs.

  vector<vector<int>> res; // 2^n-1 entries with n size each
  vector<coalition_t> all_coalition;

  vector<int> arr(problem.n,0);

  generateAllBinaryStrings(problem.n, arr, 0, res);

  coalition_t grand_coalition;
  for(int i = 0; i < problem.n; ++i)
  {
    grand_coalition.add_agent(i);
  }
  grand_coalitionresult.push_back(grand_coalition);

  for(vector<int> team : res)
  {
    coalition_t coalition;
    for(int i=0;i<problem.n;i++)
    {
      if(team[i]==1)
        coalition.add_agent(i);
    }
    all_coalition.push_back(coalition);
  }

  while(!all_coalition.empty())
  {
    //cout<<"While running"<<endl;
    // Finding Collision with largest value
    int max = -1000000;
    int max_index = -1;
    for(int i=0; i<all_coalition.size(); i++)
    {
    /*  if(i>all_coalition.size()-1)
        return grand_coalitionresult;*/
      if(problem.get_value_of_coalition(all_coalition.at(i)) > max)
      {
        max = problem.get_value_of_coalition(all_coalition.at(i));
        max_index = i;
      }
    }


    // Remove all Collision with same agents
  /*  if(max_index>all_coalition.size()-1)
      return grand_coalitionresult;*/
    coalition_t max_collision = all_coalition.at(max_index);
    vector<int> agents_in_max_collision = res.at(max_index);
    result.push_back(max_collision);

    for(int i=0;i<problem.n;i++)
    {
      if(i>all_coalition.size()-1)
        return grand_coalitionresult;
      if(agents_in_max_collision.at(i)!=1)
      {
          continue;
      }
      vector<coalition_t> all_coalition_updated{};
      vector<vector<int>> all_binaryString_updated{};
      for(int j = 0;j<all_coalition.size();j++){
        /*if(j>all_coalition.size()-1)
          return grand_coalitionresult;
        if(j>res.size()-1)
            return grand_coalitionresult;*/
        if(!(all_coalition.at(j).is_agent_in_coalition(i)))
        {
          all_coalition_updated.push_back(all_coalition.at(j));
          all_binaryString_updated.push_back(res.at(j));
        }
      }
      all_coalition = all_coalition_updated;
      res = all_binaryString_updated;
      if(all_coalition.empty())
        break;
    }
  }




  /*coalition_t grand_coalition;
  for(int i = 0; i < problem.n; ++i)
  {

    grand_coalition.add_agent(i);
  }
  result.push_back(grand_coalition);*/
  // ------------------------------------------------------------

  return result;
}

int main()
{
  // Parse input.
  int n;
  cin >> n;
  CSG_problem_t problem(n);
  for (int i = 0; i < (1 << n) - 1; ++i) {
    int v, k;
    cin >> v >> k;
    coalition_t coalition{};
    for (int j{}; j < k; ++j)
    {
      int e;
      cin >> e;
      coalition.add_agent(e - 1);
    }
    problem.set_value_of_coalition(coalition, v);
  }

  // Solve problem.
  coalition_structure_t result = solve(problem);

  // Print solution.
  cout << result.size() << endl;
  for (const coalition_t & coalition : result)
  {
    cout << coalition.get_n_agents();
    for (int i = 0; i < n; ++i)
    {
      if (coalition.is_agent_in_coalition(i))
      {
        cout << " " << i + 1;
      }
    }
    cout << endl;
  }

  return 0;
}
