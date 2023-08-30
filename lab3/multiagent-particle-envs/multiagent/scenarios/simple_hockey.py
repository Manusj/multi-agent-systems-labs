import numpy as np
from multiagent.core import World, Agent, Landmark
from multiagent.scenario import BaseScenario

class Scenario(BaseScenario):
    def make_world(self):
        world = World()
        # set any world properties first
        world.dim_c = 2
        num_agents = 2
        num_adversaries = 1
        num_landmarks = 5
        # add agents
        world.agents = [Agent() for i in range(num_agents)]
        for i, agent in enumerate(world.agents):
            agent.name = 'agent %d' % i
            agent.collide = True
            agent.silent = True
            if i < num_adversaries:
                agent.adversary = True
                agent.color = np.array([0.75, 0.25, 0.25])
            else:
                agent.adversary = False
                agent.color = np.array([0.25, 0.25, 0.75])
        # add landmarks for goal posts and puck
        goal_posts = [[-0.25, -1.0],
                      [-0.25, 1.0],
                      [0.25, -1.0],
                      [0.25, 1.0]]
        world.landmarks = [Landmark() for i in range(num_landmarks)]
        for i, landmark in enumerate(world.landmarks):
            landmark.name = 'landmark %d' % i
            if i > 0:
                landmark.collide = True
                landmark.movable = False
                landmark.state.p_pos = np.array(goal_posts[i-1])
                landmark.state.p_vel = np.zeros(world.dim_p)
            else:
                landmark.collide = True
                landmark.movable = True
        # add landmarks for rink boundary
        #world.landmarks += self.set_boundaries(world)
        # make initial conditions
        self.reset_world(world)
        return world

    def set_boundaries(self, world):
        boundary_list = []
        landmark_size = 1
        edge = 1 + landmark_size
        num_landmarks = int(edge * 2 / landmark_size)
        for x_pos in [-edge, edge]:
            for i in range(num_landmarks):
                l = Landmark()
                l.state.p_pos = np.array([x_pos, -1 + i * landmark_size])
                boundary_list.append(l)

        for y_pos in [-edge, edge]:
            for i in range(num_landmarks):
                l = Landmark()
                l.state.p_pos = np.array([-1 + i * landmark_size, y_pos])
                boundary_list.append(l)

        for i, l in enumerate(boundary_list):
            l.name = 'boundary %d' % i
            l.collide = True
            l.movable = False
            l.boundary = True
            l.color = np.array([0.75, 0.75, 0.75])
            l.size = landmark_size
            l.state.p_vel = np.zeros(world.dim_p)

        return boundary_list

    def reset_world(self, world):
        # random properties for landmarks
        for i, landmark in enumerate(world.landmarks):
            if i > 0:
                landmark.color = np.array([0.7, 0.7, 0.7])
            else:
                landmark.color = np.array([0.1, 0.1, 0.1])
            landmark.index = i
        # set random initial states
        for agent in world.agents:
            agent.state.p_pos = np.random.uniform(-1, +1, world.dim_p)
            agent.state.p_vel = np.zeros(world.dim_p)
            agent.state.c = np.zeros(world.dim_c)
        world.landmarks[0].state.p_pos = np.random.uniform(-1, +1, world.dim_p)
        world.landmarks[0].state.p_vel = np.zeros(world.dim_p)

    # return all agents of the blue team
    def blue_agents(self, world):
        return [agent for agent in world.agents if not agent.adversary]

    # return all agents of the red team
    def red_agents(self, world):
        return [agent for agent in world.agents if agent.adversary]

    def reward(self, agent, world):
        # Agents are rewarded based on team they belong to
        return self.adversary_reward(agent, world) if agent.adversary else self.agent_reward(agent, world)

    def agent_reward(self, agent, world):
        # reward for blue team agent
        blue_goal_posts = [[0.25, -1.0], [-0.25, -1.0]]
        red_goal_posts = [[0.25, 1.0], [-0.25, 1.0]]
        agent_pos = agent.state.p_pos
        agent_vel = agent.state.p_vel
        puck_pos = world.landmarks[0].state.p_pos
        puck_vel = world.landmarks[0].state.p_vel

        # print(
        #     "\nblue_goal_posts= ", blue_goal_posts,
        #     "\nred_goal_posts= " , red_goal_posts,
        #     "\nagent_pos= ", agent_pos,
        #     "\nagent_vel= ", agent_vel,
        #     "\npuck_pos= ", puck_pos,
        #     "\npuck_vel= ", puck_vel,
        #     "\n"
        # )        


        blue_goal_center = [0.0, -1.0]
        red_goal_center = [0.0, 1.0]
        epsilon = 0.00001 # very close to 0

        # puck close to red (opponent) goal
        puck_dist_to_red_goal = np.linalg.norm(red_goal_center - puck_pos) # uhh, reward for closeness to goal instead...

        
        # puck velocity (in direction of red goal)
        puck_vel_normal = 0 if np.linalg.norm(puck_vel) < epsilon else \
            puck_vel / np.linalg.norm(puck_vel)
        red_goal_vector_normal = 0 if np.linalg.norm(red_goal_center - puck_pos) < epsilon else \
            (red_goal_center - puck_pos) / np.linalg.norm(red_goal_center - puck_pos)
        c = np.dot(puck_vel_normal, red_goal_vector_normal)  # -> cosine of the angle
        angle_puck_path_goal = np.arccos(np.clip(c, -1, 1))  # smaller angle, not larger...


        # agent close to puck
        dist_to_puck = np.linalg.norm(puck_pos - agent_pos)  # uhh, reward for closeness to puck instead...
        # sqrt8 minus dist?


        # agent on "correct" side of puck (between the puck and its own goal)
        correct_side = 1 if puck_pos[1] - agent_pos[1] > 0 else 0


        # agent travelling towards puck (check agent velocity)
        agent_vel_normal = 0 if np.linalg.norm(agent_vel) < epsilon else \
            agent_vel / np.linalg.norm(agent_vel)
        puck_agent_vector = (puck_pos - agent_pos) / np.linalg.norm(puck_pos - agent_pos)
        c = np.dot(agent_vel_normal, puck_agent_vector)  # -> cosine of the angle
        angle_agent_path_puck = np.arccos(np.clip(c, -1, 1)) # smaller angle, not larger... 



        # puck in opponent goal (HIGH reward)
        puck_in_red_goal = 0
        if puck_pos[0] < red_goal_posts[0][0] and puck_pos[0] > red_goal_posts[1][0] and puck_pos[1] == 1:
            puck_in_red_goal = 1

        # seems to require higher weights for distance to puck
        # ...and maybe other tweeks

        reward_measure_weights = [
            (puck_dist_to_red_goal, 1),
            (angle_puck_path_goal, 1),
            (dist_to_puck, 1),
            (correct_side, 1),
            (angle_agent_path_puck, 1),
            (puck_in_red_goal, 100)
        ]

        reward = 0

        for entry in reward_measure_weights:
            reward += entry[0] * entry[1]

        return reward


    def adversary_reward(self, agent, world):
        # reward for red team agent
        return 0.0
               
    def observation(self, agent, world):
        # get positions/vel of all entities in this agent's reference frame
        entity_pos = []
        entity_vel = []
        for entity in world.landmarks:  # world.entities:
            entity_pos.append(entity.state.p_pos - agent.state.p_pos)
            if entity.movable:
                entity_vel.append(entity.state.p_vel)
        # get positions/vel of all other agents in this agent's reference frame
        other_pos = []
        other_vel = []
        for other in world.agents:
            if other is agent: continue
            other_pos.append(other.state.p_pos - agent.state.p_pos)
            other_vel.append(other.state.p_vel)
        return np.concatenate([agent.state.p_vel] + entity_pos + entity_vel + other_pos + other_vel)
