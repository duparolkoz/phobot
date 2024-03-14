package me.earth.phobot.pathfinder;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.Phobot;
import me.earth.phobot.pathfinder.algorithm.AStar;
import me.earth.phobot.pathfinder.algorithm.Algorithm;
import me.earth.phobot.pathfinder.mesh.MeshNode;
import me.earth.phobot.pathfinder.mesh.NavigationMeshManager;
import me.earth.phobot.pathfinder.movement.MovementPathfinder;
import me.earth.phobot.pathfinder.render.AlgorithmRenderer;
import me.earth.phobot.pathfinder.util.*;
import me.earth.phobot.services.TaskService;
import me.earth.pingbypass.PingBypass;
import me.earth.pingbypass.api.event.api.EventBus;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages Pathfinding in {@link Phobot}.
 * For more information on the pathfinding process read the documentation of the {@link MovementPathfinder}.
 *
 * @see MovementPathfinder
 */
@Slf4j
@Getter
public class Pathfinder extends MovementPathfinder {
    private static final long DEFAULT_TIME_OUT = TimeUnit.SECONDS.toMillis(10L);

    private final LevelBoundTaskManager levelBoundTaskManager = new LevelBoundTaskManager();
    private final NavigationMeshManager navigationMeshManager;
    private final ExecutorService executorService;
    private final TaskService taskService;
    private final PingBypass pingBypass;
    private final EventBus eventBus;

    public Pathfinder(PingBypass pingBypass, EventBus eventBus, NavigationMeshManager navigationMeshManager, ExecutorService executorService, TaskService taskService) {
        super(pingBypass);
        this.eventBus = eventBus;
        this.executorService = executorService;
        this.levelBoundTaskManager.getListeners().forEach(this::listen);
        this.navigationMeshManager = navigationMeshManager;
        this.taskService = taskService;
        this.pingBypass = pingBypass;
    }

    /**
     * Finds a path of {@link MeshNode}s from the current node the player is on to the given goal using {@link AStar}.
     *
     * @param player the player to get the starting position from.
     * @param goal the goal to reach.
     * @param render if you want to render the algorithm.
     * @return a {@link Process} representing the path finding process.
     */
    public CancellableFuture<Algorithm.@NotNull Result<MeshNode>> findPath(Player player, MeshNode goal, boolean render) {
        CancellableFuture<Algorithm.Result<MeshNode>> future;

        Optional<MeshNode> start = navigationMeshManager.getStartNode(player);
        if (start.isEmpty()) {
            future = new CancellableFuture<>(Cancellation.UNCANCELLABLE);
            future.completeExceptionally(new IllegalStateException("Could not find start mesh node"));
        } else {
            future = findPath(start.get(), goal, render);
        }

        return future;
    }

    /**
     * Finds a path of {@link MeshNode}s from the given start node to the give goal.
     *
     * @param start the node to start from.
     * @param goal the goal to reach.
     * @param render if you want to render the algorithm.
     * @return a {@link Process} representing the path finding process.
     */
    public CancellableFuture<Algorithm.@NotNull Result<MeshNode>> findPath(MeshNode start, MeshNode goal, boolean render) {
        CancellableFuture<Algorithm.Result<MeshNode>> future;
        var algorithm = new AStar<>(start, goal);
        future = CancellationTaskUtil.runWithTimeOut(algorithm, taskService, DEFAULT_TIME_OUT, executorService);
        future = FutureUtil.notNull(future);
        levelBoundTaskManager.addFuture(future);
        if (render) {
            AlgorithmRenderer.render(future, eventBus, algorithm);
        }

        return future;
    }

}
