package dev.corgitaco.featurerecycler;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import net.minecraft.ReportType;
import net.minecraft.ReportedException;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.Unit;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class FeatureRecycler {

    /**
     * The mod id for Feature Recycler
     */
    public static final String MOD_ID = "featurerecycler";

    /**
     * The logger for Feature Recycler
     */
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Initializes the mod.
     */
    public static void init() {

    }

    private static final ExecutorService FEATURE_RECYCLER_SERVICE = makeExecutor();


    public static <T extends Holder<Biome>> List<FeatureSorter.StepFeatureData> recycle(List<T> biomes, Function<T, List<HolderSet<PlacedFeature>>> toFeatureSetFunction) {
        long startTime = System.currentTimeMillis();
        LOGGER.info("Starting feature recycler...");

        AtomicInteger crashesPrevented = new AtomicInteger(0);

        List<Map<T, List<Holder<PlacedFeature>>>> biomeTracker = new ArrayList<>();

        for (T biome : biomes) {
            List<HolderSet<PlacedFeature>> features = toFeatureSetFunction.apply(biome);

            for (int featureStep = 0; featureStep < features.size(); featureStep++) {
                HolderSet<PlacedFeature> feature = features.get(featureStep);

                if (featureStep >= biomeTracker.size()) {
                    biomeTracker.add(new Reference2ObjectLinkedOpenHashMap<>());
                }
                biomeTracker.get(featureStep).put(biome, new ArrayList<>(feature.stream().toList()));
            }
        }


        CompletableFuture<Unit>[] futures = new CompletableFuture[biomeTracker.size()];

        for (int i = 0; i < biomeTracker.size(); i++) {
            Map<T, List<Holder<PlacedFeature>>> featuresForBiomeStage = biomeTracker.get(i);
            CompletableFuture<Unit> result = CompletableFuture.supplyAsync(() -> {
                for (int biomeIdx = 0; biomeIdx < biomes.size(); biomeIdx++) {
                    T biome = biomes.get(biomeIdx);
                    List<Holder<PlacedFeature>> currentList = featuresForBiomeStage.get(biome);
                    if (currentList == null) {
                        continue;
                    }

                    for (int currentHolderIndex = 0; currentHolderIndex < currentList.size(); currentHolderIndex++) {
                        Holder<PlacedFeature> currentHolder = currentList.get(currentHolderIndex);

                        for (int nextHolderIndex = currentHolderIndex + 1; nextHolderIndex < currentList.size(); nextHolderIndex++) {
                            Holder<PlacedFeature> nextHolder = currentList.get(nextHolderIndex);
                            int currentFeatureIDX = -1;
                            int nextFeatureIDX = -1;
                            Holder<Biome> biomeRuleSetter = null;

                            for (int previousBiomeIdx = 0; previousBiomeIdx < biomeIdx - 1; previousBiomeIdx++) {
                                T previousBiome = biomes.get(previousBiomeIdx);
                                List<Holder<PlacedFeature>> previousBiomeStageData = featuresForBiomeStage.get(previousBiome);
                                if (previousBiomeStageData == null) {
                                    continue;
                                }

                                if (currentFeatureIDX >= 0 && nextFeatureIDX >= 0) {
                                    break;
                                }

                                int previousBiomeCurrentFeatureIDX = -1;
                                int previousBiomeNextFeatureIDX = -1;
                                for (int previousBiomeHolderIdx = 0; previousBiomeHolderIdx < previousBiomeStageData.size(); previousBiomeHolderIdx++) {
                                    Holder<PlacedFeature> previousBiomePlacedFeatureHolder = previousBiomeStageData.get(previousBiomeHolderIdx);

                                    if (previousBiomePlacedFeatureHolder == currentHolder) {
                                        previousBiomeCurrentFeatureIDX = previousBiomeHolderIdx;
                                    }

                                    if (previousBiomePlacedFeatureHolder == nextHolder) {
                                        previousBiomeNextFeatureIDX = previousBiomeHolderIdx;
                                    }


                                    if (previousBiomeCurrentFeatureIDX >= 0 && previousBiomeNextFeatureIDX >= 0) {
                                        break;
                                    }
                                }

                                currentFeatureIDX = previousBiomeCurrentFeatureIDX;
                                nextFeatureIDX = previousBiomeNextFeatureIDX;
                                biomeRuleSetter = previousBiome;
                            }
                            if (currentFeatureIDX >= 0 && nextFeatureIDX >= 0) {
                                if (currentFeatureIDX > nextFeatureIDX) {
                                    ResourceLocation currentBiomeLocation = biome.unwrapKey().isEmpty() ? null : biome.unwrapKey().orElseThrow().location();
                                    String currentBiomeName = currentBiomeLocation == null ? "???" : currentBiomeLocation.toString();
                                    String currentFeatureName = currentHolder.unwrapKey().isEmpty() ? "???" : currentHolder.unwrapKey().orElseThrow().location().toString();
                                    String nextFeatureName = nextHolder.unwrapKey().isEmpty() ? "???" : nextHolder.unwrapKey().orElseThrow().location().toString();
                                    ResourceLocation ruleSetterLocation = biomeRuleSetter.unwrapKey().isEmpty() ? null : biomeRuleSetter.unwrapKey().orElseThrow().location();
                                    String biomeRuleSetterName = ruleSetterLocation == null ? "???" : ruleSetterLocation.toString();

                                    LOGGER.warn("Moved placed feature \"%s\" from index %d to index %d for biome \"%s\". Placed Feature index rules set by biome \"%s\".".formatted(currentFeatureName, currentHolderIndex, nextHolderIndex, currentBiomeName, biomeRuleSetterName));
                                    LOGGER.warn("Moved placed feature \"%s\" from index %d to index %d for biome \"%s\". Placed Feature index rules set by biome \"%s\".".formatted(nextFeatureName, nextHolderIndex, currentHolderIndex, currentBiomeName, biomeRuleSetterName));


                                    LOGGER.warn("Just prevented a crash between %s and %s! Please report the issues to their respective issue trackers.".formatted(currentBiomeLocation == null ? "???" : currentBiomeLocation.getNamespace(), ruleSetterLocation == null ? "???" : ruleSetterLocation.getNamespace()));
                                    crashesPrevented.incrementAndGet();
                                    currentList.set(currentHolderIndex, nextHolder);
                                    currentList.set(nextHolderIndex, currentHolder);
                                }
                            }
                        }
                    }
                }
                return Unit.INSTANCE;
            }, FEATURE_RECYCLER_SERVICE);
            futures[i] = result;
        }

        CompletableFuture.allOf(futures).join();

        List<FeatureSorter.StepFeatureData> steps = new ArrayList<>();

        biomeTracker.forEach(stepData -> {
            List<PlacedFeature> organizedFeatures = new ArrayList<>();

            Object2IntOpenHashMap<PlacedFeature> indexGetter = new Object2IntOpenHashMap<>();

            int idx = 0;
            for (List<Holder<PlacedFeature>> value : stepData.values()) {
                for (Holder<PlacedFeature> holder : value) {
                    organizedFeatures.add(holder.value());
                    indexGetter.put(holder.value(), idx);
                    idx++;
                }
            }
            steps.add(new FeatureSorter.StepFeatureData(organizedFeatures, indexGetter::getInt));
        });

        LOGGER.info("Finished recycling features. Took %dms".formatted(System.currentTimeMillis() - startTime));

        if (crashesPrevented.get() > 0) {
            LOGGER.info("Feature Recycler just prevented %d crashes!".formatted(crashesPrevented.get()));
        }
        return steps;
    }

    private static final AtomicInteger WORKER_COUNT = new AtomicInteger(1);

    private static ExecutorService makeExecutor() {
        return new ThreadPoolExecutor(0, GenerationStep.Decoration.values().length,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<>(), (runnable) -> {
            Thread thread = new Thread(runnable);
            thread.setName("Feature-Recycler-Worker-" + WORKER_COUNT.getAndIncrement());

            Thread.UncaughtExceptionHandler uncaughtExceptionHandler = (t, throwable) -> {
                if (throwable instanceof CompletionException) {
                    throwable = throwable.getCause();
                }

                if (throwable instanceof ReportedException) {
                    Bootstrap.realStdoutPrintln(((ReportedException) throwable).getReport().getFriendlyReport(ReportType.CRASH));
                    System.exit(-1);
                }

                LOGGER.error(String.format(Locale.ROOT, "Caught exception in thread %s", thread), throwable);
            };
            thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
            return thread;
        });
    }
}
