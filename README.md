# Intelligent-Agents-Labs
Aleksandar Hrusanov & Rastislav Kovac

## Lab 1: Rabbit-Grass Simulation
Simple simulation of the evolution of rabbit population and grass population.

## Lab 2: A Reactive Agent for the Pickup and Delivery Problem
Implementation of a reactive agent to solve the Pickup and Delivery Problem. It uses a reinforcement learning algorithm (RLA) to compute an optimal strategy off-line. This strategy is then used by the agent to travel through the network.

## Lab 3: A Deliberative Agent for the Pickup and Delivery Problem
Implementation of a deliberative agent to solve the Pickup and Delivery Problem. A deliberative agent does not simply react to percepts coming from the environment. It can build a plan that specifies the sequence of actions to be taken in
order to reach a certain goal. A deliberative agent has goals (e.g. to deliver all tasks) and
is fully aware of the world it is acting in.
Unlike the reactive agent, the deliberative agent knows the list of tasks that must be delivered. The deliberative agent can therefore construct a plan (a certain path through the network) that guarantees the optimal delivery of tasks.
Whenever a deliberative agent is called to execute a move, it applies the following internal
planning process :
1: if (Current plan is not applicable anymore) then
2: Compute optimal plan
3: end if
4: Execute the next action in the plan

## Lab 4: A Centralized Agent for the Pickup and Delivery Problem
Deliberative agents are very efficient in executing a plan, as long as they are not
disturbed by events that have not been taken into account in the plan. The presence of a second deliberative agent can make the whole
company very inefficient. The reason for this inefficiency is the lack of coordination
between deliberative agents. In the case of multi-agent systems the agents’ ability to
coordinate their actions in achieving a common goal is a vital condition for the overall
performance of the system.
The simplest form of coordination is centralized coordination, in which one entity (e.g.
the logistics company) instructs the agents how to act. In this problem centralized
coordination means that the company builds a plan for delivering all the packages
with the available vehicles, and then communicates the respective parts of the plan to
each vehicle. The vehicles simply execute the plans that were given to them.

## Lab 5: An Auctioning Agent for the Pickup and Delivery Problem
Centralized coordination is guaranteed to produce the optimal plan (globally or locally)
for a multi-agent PDP problem, when :
— All tasks are known in advance (they were until now, in our setting)
— The company has complete information about the parameters of its vehicles
— The vehicles blindly follow the orders of the company
Unfortunately, these conditions are not always met in real life applications. Most often
vehicles or group of vehicles (i.e. companies) are self-interested and might not be willing
to obey a central planner. This may happen for various reasons : for example, the central
planning might be unfair, or the agent might not wish to reveal the true information
about its state.
One intuitive solution is to allow the agents to negotiate and distribute the transportation
tasks among themselves, such that they coordinate their actions in a decentralized
fashion. A market is thus created, where the tasks are "sold" to the agent that is most
willing to take them. The competition usually leads to an efficient delivery solution.
