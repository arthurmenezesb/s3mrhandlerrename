/*
 * Copyright 2014 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.realm; 

import android.annotation.TargetApi; 
import android.content.Context; 
import android.os.Build; 
import android.os.Handler; 
import android.os.Looper; 
 
import android.util.JsonReader; 

import org.json.JSONArray; 
import org.json.JSONException; 
import org.json.JSONObject; 

 
import java.io.File; 
import java.io.IOException; 
import java.io.InputStream; 
import java.io.InputStreamReader; 
import java.lang.ref.Reference; 
import java.lang.ref.ReferenceQueue; 
import java.lang.ref.WeakReference; 
import java.lang.reflect.Constructor; 
import java.lang.reflect.InvocationTargetException; 
import java.util.ArrayList; 
 
import java.util.HashMap; 
import java.util.IdentityHashMap; 
import java.util.HashSet; 
 
import java.util.List; 
import java.util.Map; 
import java.util.Scanner; 
import java.util.Set; 
 
import java.util.concurrent.CopyOnWriteArrayList; 
import java.util.concurrent.Future; 
 
import io.realm.exceptions.RealmEncryptionNotSupportedException; 

import io.realm.exceptions.RealmException; 
import io.realm.exceptions.RealmIOException; 
import io.realm.exceptions.RealmMigrationNeededException; 
import io.realm.internal.ColumnIndices; 
 
 
import io.realm.internal.RealmObjectProxy; 
import io.realm.internal.RealmProxyMediator; 
 
import io.realm.internal.Table; 
import io.realm.internal.TableView; 
import io.realm.internal.UncheckedRow; 
import io.realm.internal.Util; 
 
import io.realm.internal.async.QueryUpdateTask; 
import io.realm.internal.async.RealmThreadPoolExecutor; 
 
import io.realm.internal.log.RealmLog; 

/**
 * The Realm class is the storage and transactional manager of your object persistent store. It
 * is in charge of creating instances of your RealmObjects. Objects within a Realm can be queried
 * and read at any time. Creating, modifying, and deleting objects must be done while inside a
 * transaction. See {@link #beginTransaction()}
 * <p>
 * The transactions ensure that multiple instances (on multiple threads) can access the same
 * objects in a consistent state with full ACID guarantees.
 * <p>
 * It is important to remember to call the {@link #close()} method when done with a Realm
 * instance. Failing to do so can lead to {@link java.lang.OutOfMemoryError} as the native
 * resources cannot be freed.
 * <p>
 * Realm instances cannot be used across different threads. This means that you have to open an
 * instance on each thread you want to use Realm. Realm instances are cached automatically per
 * thread using reference counting, so as long as the reference count doesn't reach zero, calling
 * {@link #getInstance(android.content.Context)} will just return the cached Realm and should be
 * considered a lightweight operation.
 * <p>
 * For the UI thread this means that opening and closing Realms should occur in either
 * onCreate/onDestroy or onStart/onStop.
 * <p>
 * Realm instances coordinate their state across threads using the {@link android.os.Handler}
 * mechanism. This also means that Realm instances on threads without a {@link android.os.Looper}
 * cannot receive updates unless {@link #refresh()} is manually called.
 * <p>
 * A standard pattern for working with Realm in Android activities can be seen below:
 * <p>
 * <pre>
 * public class RealmActivity extends Activity {
 *
 *   private Realm realm;
 *
 *   \@Override
 *   protected void onCreate(Bundle savedInstanceState) {
 *     super.onCreate(savedInstanceState);
 *     setContentView(R.layout.layout_main);
 *     realm = Realm.getInstance(this);
 *   }
 *
 *   \@Override
 *   protected void onDestroy() {
 *     super.onDestroy();
 *     realm.close();
 *   }
 * }
 * </pre>
 * <p>
 * Realm supports String and byte fields containing up to 16 MB.
 * <p>
 * @see <a href="http://en.wikipedia.org/wiki/ACID">ACID</a>
 * @see <a href="https://github.com/realm/realm-java/tree/master/examples">Examples using Realm</a>
 */
public final  class  Realm  extends BaseRealm   {
	
    public static final String DEFAULT_REALM_NAME = RealmConfiguration.DEFAULT_REALM_NAME;
	

    protected static final ThreadLocal<Map<RealmConfiguration, Realm>> realmsCache =
            new ThreadLocal<Map<RealmConfiguration, Realm>>() {
        @Override
        protected Map<RealmConfiguration, Realm> initialValue() {
            return new HashMap<RealmConfiguration, Realm>();
        }
    };
	

    private static final ThreadLocal<Map<RealmConfiguration, Integer>> referenceCount =
            new ThreadLocal<Map<RealmConfiguration,Integer>>() {
        @Override
        protected Map<RealmConfiguration, Integer> initialValue() {
            return new HashMap<RealmConfiguration, Integer>();
        }
    };
	

    // Map between all Realm file paths and all known configurations pointing to that file.
    
	

    // Map how many times a Realm path has been opened across all threads.
    // This is only needed by deleteRealmFile.
    
	

    
	
    <<<<<<< MINE
static final int REALM_CHANGED = 14930352;
=======
>>>>>>> YOURS

	 // Hopefully it won't clash with other message IDs.
    static final int REALM_UPDATE_ASYNC_QUERIES = 24157817;
	
    static final int REALM_COMPLETED_ASYNC_QUERY = 39088169;
	
    static final int REALM_COMPLETED_ASYNC_FIND_FIRST = 63245986;
	

    // List of Realm files that has already been validated
    private static final Set<String> validatedRealmFiles = new HashSet<String>();
	

    // keep a WeakReference list to RealmResults obtained asynchronously in order to update them
    // RealmQuery is not WeakReference to prevent it from being GC'd. RealmQuery should be
    // cleaned if RealmResults is cleaned. we need to keep RealmQuery because it contains the query
    // pointer (to handover for each update) + all the arguments necessary to rerun the query:
    // sorting orders, soring columns, type (findAll, findFirst, findAllSorted etc.)
    final Map<WeakReference<RealmResults<?>>, RealmQuery<?>> asyncRealmResults =
            new IdentityHashMap<WeakReference<RealmResults<?>>, RealmQuery<?>>();
	
    final ReferenceQueue<RealmResults<?>> referenceQueue = new ReferenceQueue<RealmResults<?>>();
	
    final Map<WeakReference<RealmObject>, RealmQuery<?>> asyncRealmObjects =
            new IdentityHashMap<WeakReference<RealmObject>, RealmQuery<?>>();
	

    // thread pool for all async operations (Query & Write transaction)
    static final RealmThreadPoolExecutor asyncQueryExecutor = RealmThreadPoolExecutor.getInstance();
	
    // pending update of async queries
    private Future updateAsyncQueriesTask;
	

    // Caches Class objects (both model classes and proxy classes) to Realm Tables
    private final Map<Class<? extends RealmObject>, Table> classToTable =
            new HashMap<Class<? extends RealmObject>, Table>();
	

    
	
    
	
    
	
    
	

    
	
    
	
    
	

    
	
    
	
    <<<<<<< MINE
protected SharedGroup sharedGroup;
=======
>>>>>>> YOURS

	
    
	

    
	

    
	

    private static RealmConfiguration defaultConfiguration;
	

    protected ColumnIndices columnIndices = new ColumnIndices();
	

    static {
        RealmLog.add(BuildConfig.DEBUG ? new DebugAndroidLogger() : new ReleaseAndroidLogger());
    }
	

    /**
     * The constructor is private to enforce the use of the static one.
     *
     * @param configuration Configuration used to open the Realm.
     * @param autoRefresh {@code true} if Realm should auto-refresh. {@code false} otherwise.
     * @throws IllegalArgumentException if trying to open an encrypted Realm with the wrong key.
     * @throws RealmEncryptionNotSupportedException if the device doesn't support Realm encryption.
     */
    private Realm(RealmConfiguration configuration, boolean autoRefresh) {
        super(configuration, autoRefresh);
    }
	

    @Override
    protected void finalize() throws Throwable {
        if (sharedGroupManager != null && sharedGroupManager.isOpen()) {
            RealmLog.w("Remember to call close() on all Realm instances. " +
                            "Realm " + configuration.getPath() + " is being finalized without being closed, " +
                            "this can lead to running out of native memory."
            );
        }
        super.finalize();
    }
	

    /**
     * Closes the Realm instance and all its resources.
     * <p>
     * It's important to always remember to close Realm instances when you're done with it in order
     * not to leak memory, file descriptors or grow the size of Realm file out of measure.
     *
     * @throws java.lang.IllegalStateException if trying to close Realm on a different thread than the
     * one it was created on.
     */
    @Override
    public void close() {
        if (this.threadId != Thread.currentThread().getId()) {
            throw new IllegalStateException(INCORRECT_THREAD_CLOSE_MESSAGE);
        }

        Map<RealmConfiguration, Integer> localRefCount = referenceCount.get();
        String canonicalPath = configuration.getPath();
        Integer references = localRefCount.get(configuration);
        if (references == null) {
            references = 0;
        }
        if (sharedGroup != null && references == 1) {
            realmsCache.get().remove(configuration);
            sharedGroup.close();
            sharedGroup = null;

            // It is necessary to be synchronized here since there is a chance that before the counter removed,
            // the other thread could get the counter and increase it in createAndValidate.
            synchronized (Realm.class) {
                validatedRealmFiles.remove(configuration.getPath());
                List<RealmConfiguration>  pathConfigurationCache = globalPathConfigurationCache.get(canonicalPath);
                pathConfigurationCache.remove(configuration);
                if (pathConfigurationCache.isEmpty()) {
                    globalPathConfigurationCache.remove(canonicalPath);
                }
                AtomicInteger counter = globalOpenInstanceCounter.get(canonicalPath);
                if (counter.decrementAndGet() == 0) {
                    globalOpenInstanceCounter.remove(canonicalPath);
                }
            }
        }

        int refCount = references - 1;
        if (refCount < 0) {
            RealmLog.w("Calling close() on a Realm that is already closed: " + canonicalPath);
        }
        localRefCount.put(configuration, Math.max(0, refCount));

        if (handler != null && refCount <= 0) {
            removeHandler(handler);
            handler = null;
        }
    }

	

    
	

        RealmCallback   {
		
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case REALM_CHANGED: {
                    if (threadContainsAsyncQueries()) {
                        if (updateAsyncQueriesTask != null && !updateAsyncQueriesTask.isDone()) {
                            // try to cancel any pending update since we're submitting a new one anyway
                            updateAsyncQueriesTask.cancel(true);
                            asyncQueryExecutor.getQueue().remove(updateAsyncQueriesTask);
                            RealmLog.d("REALM_CHANGED realm:"+ Realm.this + " cancelling pending REALM_UPDATE_ASYNC_QUERIES updates");
                        }
                        RealmLog.d("REALM_CHANGED realm:"+ Realm.this + " updating async queries, total: " + asyncRealmResults.size());
                        // prepare a QueryUpdateTask to current async queries in this thread
                        QueryUpdateTask.Builder.UpdateQueryStep updateQueryStep = QueryUpdateTask.newBuilder()
                                .realmConfiguration(getConfiguration());
                        QueryUpdateTask.Builder.RealmResultsQueryStep realmResultsQueryStep = null;

                        // we iterate over non GC'd async RealmResults then add them to the list to be updated (in a batch)
                        Iterator<Map.Entry<WeakReference<RealmResults<?>>, RealmQuery<?>>> iterator = asyncRealmResults.entrySet().iterator();
                        while (iterator.hasNext()) {
                            Map.Entry<WeakReference<RealmResults<?>>, RealmQuery<?>> entry = iterator.next();
                            WeakReference<RealmResults<?>> weakReference = entry.getKey();
                            RealmResults<?> realmResults = weakReference.get();
                            if (realmResults == null) {
                                // GC'd instance remove from the list
                                iterator.remove();

                            } else {
                                realmResultsQueryStep = updateQueryStep.add(weakReference,
                                                  entry.getValue().handoverQueryPointer(),
                                                  entry.getValue().getArgument());
                            }

                            // Note: we're passing an WeakRef of a RealmResults to another thread
                            //       this is safe as long as we don't invoke any of the RealmResults methods.
                            //       we're just using it as a Key in an IdentityHashMap (i.e doesn't call
                            //       AbstractList's hashCode, that require accessing objects from another thread)
                            //
                            //       watch out when you debug, as you're IDE try to evaluate RealmResults
                            //       which break the Thread confinement constraints.
                        }
                        if (realmResultsQueryStep != null) {
                            QueryUpdateTask queryUpdateTask = realmResultsQueryStep
                                    .sendToHandler(handler, REALM_UPDATE_ASYNC_QUERIES)
                                    .build();
                            updateAsyncQueriesTask = asyncQueryExecutor.submit(queryUpdateTask);
                        }

                    } else {
                        RealmLog.d("REALM_CHANGED realm:"+ Realm.this + " no async queries, advance_read");
                        transaction.advanceRead();
                        sendNotifications();
                    }
                    break;
                }
                case REALM_COMPLETED_ASYNC_QUERY: {
                    // one async query has completed
                    QueryUpdateTask.Result result
                            = (QueryUpdateTask.Result) message.obj;

                    Set<WeakReference<RealmResults<?>>> updatedTableViewsKeys = result.updatedTableViews.keySet();
                    if (updatedTableViewsKeys.size() > 0) {
                        WeakReference<RealmResults<?>> weakRealmResults = updatedTableViewsKeys.iterator().next();

                        RealmResults<?> realmResults = weakRealmResults.get();
                        if (realmResults == null) {
                            asyncRealmResults.remove(weakRealmResults);
                            RealmLog.d("[REALM_COMPLETED_ASYNC_QUERY "+ weakRealmResults + "] realm:"+ Realm.this + " RealmResults GC'd ignore results");

                        } else {
                            SharedGroup.VersionID callerVersionID = sharedGroup.getVersion();
                            int compare = callerVersionID.compareTo(result.versionID);
                            if (compare == 0) {
                                // if the RealmResults is empty (has not completed yet) then use the value
                                // otherwise a task (grouped update) has already updated this RealmResults
                                if (!realmResults.isLoaded()) {
                                    RealmLog.d("[REALM_COMPLETED_ASYNC_QUERY "+ weakRealmResults + "] , realm:"+ Realm.this + " same versions, using results (RealmResults is not loaded)");
                                    // swap pointer
                                    realmResults.swapTableViewPointer(result.updatedTableViews.get(weakRealmResults));
                                    // notify callbacks
                                    realmResults.notifyChangeListeners();
                                } else {
                                    RealmLog.d("[REALM_COMPLETED_ASYNC_QUERY "+ weakRealmResults + "] , realm:"+ Realm.this + " ignoring result the RealmResults (is already loaded)");
                                }

                            } else if (compare > 0) {
                                // we have two use cases:
                                // 1- this RealmResults is not empty, this means that after we started the async
                                //    query, we received a REALM_CHANGE that triggered an update of all async queries
                                //    including the last async submitted, so no need to use the provided TableView pointer
                                //    (or the user forced the sync behaviour .load())
                                // 2- This RealmResults is still empty but this caller thread is advanced than the worker thread
                                //    this could happen if the current thread advanced the shared_group (via a write or refresh)
                                //    this means that we need to rerun the query against a newer worker thread.

                                if (!realmResults.isLoaded()) { // UC2
                                    // UC covered by this test: RealmAsyncQueryTests#testFindAllAsyncRetry
                                    RealmLog.d("[REALM_COMPLETED_ASYNC_QUERY " + weakRealmResults + "] , realm:"+ Realm.this + " caller is more advanced & RealmResults is not loaded, rerunning the query against the latest version");

                                    RealmQuery<?> query = asyncRealmResults.get(weakRealmResults);
                                    QueryUpdateTask queryUpdateTask = QueryUpdateTask.newBuilder()
                                            .realmConfiguration(getConfiguration())
                                            .add(weakRealmResults,
                                                    query.handoverQueryPointer(),
                                                    query.getArgument())
                                            .sendToHandler(handler, REALM_COMPLETED_ASYNC_QUERY)
                                            .build();

                                    asyncQueryExecutor.submit(queryUpdateTask);

                                } else {
                                    // UC covered by this test: RealmAsyncQueryTests#testFindAllCallerIsAdvanced
                                    RealmLog.d("[REALM_COMPLETED_ASYNC_QUERY "+ weakRealmResults + "] , realm:"+ Realm.this + " caller is more advanced & RealmResults is loaded ignore the outdated result");
                                }

                            } else {
                                // the caller thread is behind the worker thread,
                                // no need to rerun the query, since we're going to receive the update signal
                                // & batch update all async queries including this one
                                // UC covered by this test: RealmAsyncQueryTests#testFindAllCallerThreadBehind
                                RealmLog.d("[REALM_COMPLETED_ASYNC_QUERY "+ weakRealmResults + "] , realm:"+ Realm.this + " caller thread behind worker thread, ignore results (a batch update will update everything including this query)");
                            }
                        }
                    }
                    break;
                }
                case REALM_UPDATE_ASYNC_QUERIES: {
                    // this is called once the background thread completed the update of the async queries
                    QueryUpdateTask.Result result
                            = (QueryUpdateTask.Result) message.obj;

                    SharedGroup.VersionID callerVersionID = sharedGroup.getVersion();
                    int compare = callerVersionID.compareTo(result.versionID);
                    if (compare > 0) {
                        RealmLog.d("REALM_UPDATE_ASYNC_QUERIES realm:"+ Realm.this + " caller is more advanced, rerun updates");
                        // The caller is more advance than the updated queries ==>
                        // need to refresh them again (if there is still queries)
                        handler.sendEmptyMessage(REALM_CHANGED);

                    } else {
                        // We're behind or on the same version as the worker thread

                        // only advance if we're behind
                        if (compare != 0) {
                            // no need to remove old pointers from TableView, since they're
                            // imperative TV, they will not rerun if the SharedGroup advance

                            // UC covered by this test: RealmAsyncQueryTests#testFindAllCallerThreadBehind
                            RealmLog.d("REALM_UPDATE_ASYNC_QUERIES realm:"+ Realm.this + " caller is behind  advance_read");
                            // refresh the Realm to the version provided by the worker thread
                            // (advanceRead to the latest version may cause a version mismatch error) preventing us
                            // from importing correctly the handover table view
                            transaction.advanceRead(result.versionID);
                        }

                        ArrayList<RealmResults<?>> callbacksToNotify = new ArrayList<RealmResults<?>>(result.updatedTableViews.size());
                        // use updated TableViews pointers for the existing async RealmResults
                        for (Map.Entry<WeakReference<RealmResults<?>>, Long> query : result.updatedTableViews.entrySet()) {
                            WeakReference<RealmResults<?>> weakRealmResults = query.getKey();
                            RealmResults<?> realmResults = weakRealmResults.get();
                            if (realmResults == null) {
                                // don't update GC'd instance
                                asyncRealmResults.remove(weakRealmResults);

                            } else {
                                // it's dangerous to notify the callback about new results before updating
                                // the pointers, because the callback may use another RealmResults not updated yet
                                // this is why we defer the notification until we're done updating all pointers

                                // TODO find a way to only notify callbacks if the underlying data changed compared
                                //      to the existing value(s) for this RealmResults (use a hashCode?)
                                callbacksToNotify.add(realmResults);

                                RealmLog.d("REALM_UPDATE_ASYNC_QUERIES realm:"+ Realm.this + " updating RealmResults " + weakRealmResults);
                                // update the instance with the new pointer
                                realmResults.swapTableViewPointer(query.getValue());
                            }
                        }

                        for (RealmResults<?> query : callbacksToNotify) {
                            query.notifyChangeListeners();
                        }

                        // notify listeners only when we advanced
                        if (compare != 0) {
                            sendNotifications();
                        }

                        updateAsyncQueriesTask = null;
                    }
                    break;
                }
                case REALM_COMPLETED_ASYNC_FIND_FIRST: {
                    QueryUpdateTask.Result result = (QueryUpdateTask.Result) message.obj;
                    Set<WeakReference<RealmObject>> updatedRowKey = result.updatedRow.keySet();
                    if (updatedRowKey.size() > 0) {
                        WeakReference<RealmObject> realmObjectWeakReference = updatedRowKey.iterator().next();
                        RealmObject realmObject = realmObjectWeakReference.get();

                        if (realmObject != null) {
                            SharedGroup.VersionID callerVersionID = sharedGroup.getVersion();
                            int compare = callerVersionID.compareTo(result.versionID);
                            // we always query on the same version
                            // only two use cases could happen 1. we're on the same version or 2. the caller has advanced in the meanwhile
                            if (compare == 0) { //same version import the handover
                                realmObject.onCompleted(result.updatedRow.get(realmObjectWeakReference));
                                asyncRealmObjects.remove(realmObjectWeakReference);

                            } else if (compare > 0) {
                                // the caller has advanced we need to
                                // retry against the current version of the caller
                                RealmQuery<?> realmQuery = asyncRealmObjects.get(realmObjectWeakReference);

                                QueryUpdateTask queryUpdateTask = QueryUpdateTask.newBuilder()
                                        .realmConfiguration(getConfiguration())
                                        .addObject(realmObjectWeakReference,
                                                realmQuery.handoverQueryPointer(),
                                                realmQuery.getArgument())
                                        .sendToHandler(handler, REALM_COMPLETED_ASYNC_FIND_FIRST)
                                        .build();

                                asyncQueryExecutor.submit(queryUpdateTask);
                            } else {
                                // should not happen, since the the background thread position itself against the provided version
                                // and the caller thread can only go forward (advance_read)
                                throw new IllegalStateException("Caller thread behind the worker thread");
                            }
                        } // else: element GC'd in the meanwhile
                    }
                    break;
                }
            }
            return true;
        }


	}
	

    /**
     * Retrieve the auto-refresh status of the Realm instance.
     * @return the auto-refresh status
     */
    
	

    /**
     * Indicates if there are currently any RealmResults being used in this thread (obtained
     * previously asynchronously). This will prevent advanceRead accidentally the current transaction
     * resulting in re-running the queries in this thread.
     * @return {@code true} if there is at least one (non GC'd) instance of {@link RealmResults} {@code false} otherwise
     */
    private boolean threadContainsAsyncQueries () {
        deleteWeakReferences();
        boolean isEmpty = true;
        Iterator<Map.Entry<WeakReference<RealmResults<?>>, RealmQuery<?>>> iterator = asyncRealmResults.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<WeakReference<RealmResults<?>>, RealmQuery<?>> next = iterator.next();
            if (next.getKey().get() == null) {
                // clean the GC'd instance
                // we could've avoided this if we had a 'WeakIdentityHashmap' data structure. miss Guava :(
                iterator.remove();
            } else {
                isEmpty = false;
            }
        }

        return !isEmpty;
    }
	

    private void deleteWeakReferences() {
        // From the AOSP FinalizationTest:
        // https://android.googlesource.com/platform/libcore/+/master/support/src/test/java/libcore/
        // java/lang/ref/FinalizationTester.java
        // System.gc() does not garbage collect every time. Runtime.gc() is
        // more likely to perform a gc.
        Runtime.getRuntime().gc();
        Reference<? extends RealmResults<?>> weakReference;
        while ((weakReference = referenceQueue.poll()) != null ) { // Does not wait for a reference to become available.
            RealmLog.d("deleted 1 reference: " + weakReference);
            asyncRealmResults.remove(weakReference);
        }
    }
	

    /**
     * Set the auto-refresh status of the Realm instance.
     * <p>
     * Auto-refresh is a feature that enables automatic update of the current Realm instance and all its derived objects
     * (RealmResults and RealmObjects instances) when a commit is performed on a Realm acting on the same file in another thread.
     * This feature is only available if the Realm instance lives is a {@link android.os.Looper} enabled thread.
     *
     * @param autoRefresh true will turn auto-refresh on, false will turn it off.
     * @throws java.lang.IllegalStateException if trying to enable auto-refresh in a thread without Looper.
     */
    
	

    /**
     * Realm static constructor for the default Realm "default.realm".
     * {@link #close()} must be called when you are done using the Realm instance.
     * <p>
     * It sets auto-refresh on if the current thread has a Looper, off otherwise.
     *
     * This is equivalent to calling {@code Realm.getInstance(new RealmConfiguration(getContext()).build()) }.

     * @param context an Android {@link android.content.Context}
     * @return an instance of the Realm class
     * @throws RealmMigrationNeededException The model classes have been changed and the Realm
     *                                       must be migrated
     * @throws RealmIOException              Error when accessing underlying file
     * @throws RealmException                Other errors
     */
    public static Realm getInstance(Context context) {
        return Realm.getInstance(new RealmConfiguration.Builder(context)
                    .name(DEFAULT_REALM_NAME)
                    .build());
    }
	

    /**
     * Realm static constructor that returns the Realm instance defined by the {@link io.realm.RealmConfiguration} set
     * by {@link #setDefaultConfiguration(RealmConfiguration)}
     *
     * @return an instance of the Realm class
     *
     * @throws java.lang.NullPointerException If no default configuration has been defined.
     * @throws RealmMigrationNeededException If no migration has been provided by the default configuration and the
     * model classes or version has has changed so a migration is required.
     */
    public static Realm getDefaultInstance() {
        if (defaultConfiguration == null) {
            throw new NullPointerException("No default RealmConfiguration was found. Call setDefaultConfiguration() first");
        }
        return create(defaultConfiguration);
    }
	

    /**
     * Realm static constructor that returns the Realm instance defined by provided {@link io.realm.RealmConfiguration}
     *
     * @return an instance of the Realm class
     *
     * @throws RealmMigrationNeededException If no migration has been provided by the configuration and the
     * model classes or version has has changed so a migration is required.
     * @throws RealmEncryptionNotSupportedException if the device doesn't support Realm encryption.
     * @see RealmConfiguration for details on how to configure a Realm.
     */
    public static Realm getInstance(RealmConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("A non-null RealmConfiguration must be provided");
        }
        return create(configuration);
    }
	

    /**
     * Sets the {@link io.realm.RealmConfiguration} used when calling {@link #getDefaultInstance()}.
     *
     * @param configuration RealmConfiguration to use as the default configuration.
     * @see RealmConfiguration for details on how to configure a Realm.
     */
    public static void setDefaultConfiguration(RealmConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("A non-null RealmConfiguration must be provided");
        }
        defaultConfiguration = configuration;
    }
	

    /**
     * Removes the current default configuration (if any). Any further calls to {@link #getDefaultInstance()} will
     * fail until a new default configuration has been set using {@link #setDefaultConfiguration(RealmConfiguration)}.
     */
    public static void removeDefaultConfiguration() {
        defaultConfiguration = null;
    }
	

    private static synchronized Realm create(RealmConfiguration configuration) {
        boolean autoRefresh = Looper.myLooper() != null;
        try {
            boolean validateSchema = !validatedRealmFiles.contains(configuration.getPath());
            return createAndValidate(configuration, validateSchema, autoRefresh);

        } catch (RealmMigrationNeededException e) {
            if (configuration.shouldDeleteRealmIfMigrationNeeded()) {
                deleteRealm(configuration);
            } else {
                migrateRealm(configuration);
            }

            return createAndValidate(configuration, true, autoRefresh);
        }
    }
	

    private static Realm createAndValidate(RealmConfiguration configuration, boolean validateSchema, boolean autoRefresh) {
        synchronized (BaseRealm.class) {
        // Check if a cached instance already exists for this thread
        String canonicalPath = configuration.getPath();
        Map<RealmConfiguration, Integer> localRefCount = referenceCount.get();
        Integer references = localRefCount.get(configuration);
        if (references == null) {
            references = 0;
        }
        Map<RealmConfiguration, Realm> realms = realmsCache.get();
        Realm realm = realms.get(configuration);
        if (realm != null) {
            localRefCount.put(configuration, references + 1);
            return realm;
        }

        // Create new Realm and cache it. All exception code paths must close the Realm otherwise we risk serving
        // faulty cache data.
        validateAgainstExistingConfigurations(configuration);
        realm = new Realm(configuration, autoRefresh);
        List<RealmConfiguration> pathConfigurationCache = globalPathConfigurationCache.get(canonicalPath);
        if (pathConfigurationCache == null) {
            pathConfigurationCache = new CopyOnWriteArrayList<RealmConfiguration>();
            globalPathConfigurationCache.put(canonicalPath, pathConfigurationCache);
        }
        pathConfigurationCache.add(configuration);
        realms.put(configuration, realm);
        localRefCount.put(configuration, references + 1);

        // Increment global reference counter
            realm.acquireFileReference(configuration);

        // Check versions of Realm
        long currentVersion = realm.getVersion();
        long requiredVersion = configuration.getSchemaVersion();
        if (currentVersion != UNVERSIONED && currentVersion < requiredVersion && validateSchema) {
            realm.close();
                throw new RealmMigrationNeededException(configuration.getPath(), String.format("Realm on disk need to migrate from v%s to v%s", currentVersion, requiredVersion));
        }
        if (currentVersion != UNVERSIONED && requiredVersion < currentVersion && validateSchema) {
            realm.close();
                throw new IllegalArgumentException(String.format("Realm on disk is newer than the one specified: v%s vs. v%s", currentVersion, requiredVersion));
        }

        // Initialize Realm schema if needed
        if (validateSchema) {
            try {
                initializeRealm(realm);
            } catch (RuntimeException e) {
                realm.close();
                throw e;
            }
        }

        return realm;
    }
    }
	

    // Make sure that the new configuration doesn't clash with any existing configurations for the Realm
    
	

    @SuppressWarnings("unchecked")
    private static void initializeRealm(Realm realm) {
        long version = realm.getVersion();
        boolean commitNeeded = false;
        try {
            realm.beginTransaction();
            if (version == UNVERSIONED) {
                commitNeeded = true;
                realm.setVersion(realm.configuration.getSchemaVersion());
            }

            RealmProxyMediator mediator = realm.configuration.getSchemaMediator();
            for (Class<? extends RealmObject> modelClass : mediator.getModelClasses()) {
                // Create and validate table
                if (version == UNVERSIONED) {
                    mediator.createTable(modelClass, realm.sharedGroupManager.getTransaction());
                }
                mediator.validateTable(modelClass, realm.sharedGroupManager.getTransaction());
                realm.columnIndices.addClass(modelClass, mediator.getColumnIndices(modelClass));
            }
            validatedRealmFiles.add(realm.getPath());
        } finally {
            if (commitNeeded) {
                realm.commitTransaction();
            } else {
                realm.cancelTransaction();
            }
        }
    }
	

    
	

    /**
     * Create a Realm object for each object in a JSON array. This must be done within a transaction.
     * JSON properties with a null value will map to the default value for the data type in Realm
     * and unknown properties will be ignored.
     *
     * @param clazz Type of Realm objects to create.
     * @param json  Array where each JSONObject must map to the specified class.
     *
     * @throws RealmException if mapping from JSON fails.
     */
    public <E extends RealmObject> void createAllFromJson(Class<E> clazz, JSONArray json) {
        if (clazz == null || json == null) {
            return;
        }

        for (int i = 0; i < json.length(); i++) {
            try {
                configuration.getSchemaMediator().createOrUpdateUsingJsonObject(clazz, this, json.getJSONObject(i), false);
            } catch (Exception e) {
                throw new RealmException("Could not map Json", e);
            }
        }
    }
	

    /**
     * Tries to update a list of existing objects identified by their primary key with new JSON data. If an existing
     * object could not be found in the Realm, a new object will be created. This must happen within a transaction.
     *
     * @param clazz Type of {@link io.realm.RealmObject} to create or update. It must have a primary key defined.
     * @param json  Array with object data.
     *
     * @throws java.lang.IllegalArgumentException if trying to update a class without a
     * {@link io.realm.annotations.PrimaryKey}.
     * @see #createAllFromJson(Class, org.json.JSONArray)
     */
    public <E extends RealmObject> void createOrUpdateAllFromJson(Class<E> clazz, JSONArray json) {
        if (clazz == null || json == null) {
            return;
        }
        checkHasPrimaryKey(clazz);
        for (int i = 0; i < json.length(); i++) {
            try {
                configuration.getSchemaMediator().createOrUpdateUsingJsonObject(clazz, this, json.getJSONObject(i), true);
            } catch (Exception e) {
                throw new RealmException("Could not map Json", e);
            }
        }
    }
	

    /**
     * Create a Realm object for each object in a JSON array. This must be done within a transaction.
     * JSON properties with a null value will map to the default value for the data type in Realm
     * and unknown properties will be ignored.
     *
     * @param clazz Type of Realm objects to create.
     * @param json  JSON array as a String where each object can map to the specified class.
     *
     * @throws RealmException if mapping from JSON fails.
     */
    public <E extends RealmObject> void createAllFromJson(Class<E> clazz, String json) {
        if (clazz == null || json == null || json.length() == 0) {
            return;
        }

        JSONArray arr;
        try {
            arr = new JSONArray(json);
        } catch (Exception e) {
            throw new RealmException("Could not create JSON array from string", e);
        }

        createAllFromJson(clazz, arr);
    }
	

    /**
     * Tries to update a list of existing objects identified by their primary key with new JSON data. If an existing
     * object could not be found in the Realm, a new object will be created. This must happen within a transaction.
     *
     * @param clazz Type of {@link io.realm.RealmObject} to create or update. It must have a primary key defined.
     * @param json  String with an array of JSON objects.
     *
     * @throws java.lang.IllegalArgumentException if trying to update a class without a
     * {@link io.realm.annotations.PrimaryKey}.
     * @see #createAllFromJson(Class, String)
     */
    public <E extends RealmObject> void createOrUpdateAllFromJson(Class<E> clazz, String json) {
        if (clazz == null || json == null || json.length() == 0) {
            return;
        }
        checkHasPrimaryKey(clazz);

        JSONArray arr;
        try {
            arr = new JSONArray(json);
        } catch (JSONException e) {
            throw new RealmException("Could not create JSON array from string", e);
        }

        createOrUpdateAllFromJson(clazz, arr);
    }
	

    /**
     * Create a Realm object for each object in a JSON array. This must be done within a transaction.
     * JSON properties with a null value will map to the default value for the data type in Realm
     * and unknown properties will be ignored.
     *
     * @param clazz         Type of Realm objects created.
     * @param inputStream   JSON array as a InputStream. All objects in the array must be of the
     *                      specified class.
     *
     * @throws RealmException if mapping from JSON fails.
     * @throws IOException if something was wrong with the input stream.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public <E extends RealmObject> void createAllFromJson(Class<E> clazz, InputStream inputStream) throws IOException {
        if (clazz == null || inputStream == null) {
            return;
        }

        JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
        try {
            reader.beginArray();
            while (reader.hasNext()) {
                configuration.getSchemaMediator().createUsingJsonStream(clazz, this, reader);
            }
            reader.endArray();
        } finally {
            reader.close();
        }
    }
	

    /**
     * Tries to update a list of existing objects identified by their primary key with new JSON data. If an existing
     * object could not be found in the Realm, a new object will be created. This must happen within a transaction.
     *
     * @param clazz Type of {@link io.realm.RealmObject} to create or update. It must have a primary key defined.
     * @param in    InputStream with a list of object data in JSON format.
     *
     * @throws java.lang.IllegalArgumentException if trying to update a class without a
     * {@link io.realm.annotations.PrimaryKey}.
     * @see #createOrUpdateAllFromJson(Class, java.io.InputStream)
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public <E extends RealmObject> void createOrUpdateAllFromJson(Class<E> clazz, InputStream in) throws IOException {
        if (clazz == null || in == null) {
            return;
        }
        checkHasPrimaryKey(clazz);

        // As we need the primary key value we have to first parse the entire input stream as in the general
        // case that value might be the last property :(
        Scanner scanner = null;
        try {
            scanner = getFullStringScanner(in);
            JSONArray json = new JSONArray(scanner.next());
            for (int i = 0; i < json.length(); i++) {
                configuration.getSchemaMediator().createOrUpdateUsingJsonObject(clazz, this, json.getJSONObject(i), true);
            }
        } catch (JSONException e) {
            throw new RealmException("Failed to read JSON", e);
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
    }
	

    /**
     * Create a Realm object pre-filled with data from a JSON object. This must be done inside a
     * transaction. JSON properties with a null value will map to the default value for the data
     * type in Realm and unknown properties will be ignored.
     *
     * @param clazz Type of Realm object to create.
     * @param json  JSONObject with object data.
     * @return Created object or null if no json data was provided.
     *
     * @throws RealmException if the mapping from JSON fails.
     * @see #createOrUpdateObjectFromJson(Class, org.json.JSONObject)
     */
    public <E extends RealmObject> E createObjectFromJson(Class<E> clazz, JSONObject json) {
        if (clazz == null || json == null) {
            return null;
        }

        try {
            return configuration.getSchemaMediator().createOrUpdateUsingJsonObject(clazz, this, json, false);
        } catch (Exception e) {
            throw new RealmException("Could not map Json", e);
        }
    }
	

    /**
     * Tries to update an existing object defined by its primary key with new JSON data. If no existing object could be
     * found a new object will be saved in the Realm. This must happen within a transaction.
     *
     * @param clazz Type of {@link io.realm.RealmObject} to create or update. It must have a primary key defined.
     * @param json  {@link org.json.JSONObject} with object data.
     * @return Created or updated {@link io.realm.RealmObject}.
     * @throws java.lang.IllegalArgumentException if trying to update a class without a
     * {@link io.realm.annotations.PrimaryKey}.
     * @see #createObjectFromJson(Class, org.json.JSONObject)
     */
    public <E extends RealmObject> E createOrUpdateObjectFromJson(Class<E> clazz, JSONObject json) {
        if (clazz == null || json == null) {
            return null;
        }
        checkHasPrimaryKey(clazz);
        try {
            return configuration.getSchemaMediator().createOrUpdateUsingJsonObject(clazz, this, json, true);
        } catch (JSONException e) {
            throw new RealmException("Could not map Json", e);
        }
    }
	

    /**
     * Create a Realm object pre-filled with data from a JSON object. This must be done inside a
     * transaction. JSON properties with a null value will map to the default value for the data
     * type in Realm and unknown properties will be ignored.
     *
     * @param clazz Type of Realm object to create.
     * @param json  JSON string with object data.
     * @return Created object or null if json string was empty or null.
     *
     * @throws RealmException if mapping to json failed.
     */
    public <E extends RealmObject> E createObjectFromJson(Class<E> clazz, String json) {
        if (clazz == null || json == null || json.length() == 0) {
            return null;
        }

        JSONObject obj;
        try {
            obj = new JSONObject(json);
        } catch (Exception e) {
            throw new RealmException("Could not create Json object from string", e);
        }

        return createObjectFromJson(clazz, obj);
    }
	

    /**
     * Tries to update an existing object defined by its primary key with new JSON data. If no existing object could be
     * found a new object will be saved in the Realm. This must happen within a transaction.
     *
     * @param clazz Type of {@link io.realm.RealmObject} to create or update. It must have a primary key defined.
     * @param json  String with object data in JSON format.
     * @return Created or updated {@link io.realm.RealmObject}.
     * @throws java.lang.IllegalArgumentException if trying to update a class without a
     * {@link io.realm.annotations.PrimaryKey}.
     *
     * @see #createObjectFromJson(Class, String)
     */
    public <E extends RealmObject> E createOrUpdateObjectFromJson(Class<E> clazz, String json) {
        if (clazz == null || json == null || json.length() == 0) {
            return null;
        }
        checkHasPrimaryKey(clazz);

        JSONObject obj;
        try {
            obj = new JSONObject(json);
        } catch (Exception e) {
            throw new RealmException("Could not create Json object from string", e);
        }

        return createOrUpdateObjectFromJson(clazz, obj);
    }
	

    /**
     * Create a Realm object pre-filled with data from a JSON object. This must be done inside a
     * transaction. JSON properties with a null value will map to the default value for the data
     * type in Realm and unknown properties will be ignored.
     *
     * @param clazz         Type of Realm object to create.
     * @param inputStream   JSON object data as a InputStream.
     * @return Created object or null if json string was empty or null.
     *
     * @throws RealmException if the mapping from JSON failed.
     * @throws IOException if something was wrong with the input stream.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public <E extends RealmObject> E createObjectFromJson(Class<E> clazz, InputStream inputStream) throws IOException {
        if (clazz == null || inputStream == null) {
            return null;
        }

        JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
        try {
            return configuration.getSchemaMediator().createUsingJsonStream(clazz, this, reader);
        } finally {
            reader.close();
        }
    }
	

    /**
     * Tries to update an existing object defined by its primary key with new JSON data. If no existing object could be
     * found a new object will be saved in the Realm. This must happen within a transaction.
     *
     * @param clazz Type of {@link io.realm.RealmObject} to create or update. It must have a primary key defined.
     * @param in    {@link InputStream} with object data in JSON format.
     * @return Created or updated {@link io.realm.RealmObject}.
     * @throws java.lang.IllegalArgumentException if trying to update a class without a
     * {@link io.realm.annotations.PrimaryKey}.
     * @see #createObjectFromJson(Class, java.io.InputStream)
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public <E extends RealmObject> E createOrUpdateObjectFromJson(Class<E> clazz, InputStream in) throws IOException {
        if (clazz == null || in == null) {
            return null;
        }
        checkHasPrimaryKey(clazz);

        // As we need the primary key value we have to first parse the entire input stream as in the general
        // case that value might be the last property :(
        Scanner scanner = null;
        try {
            scanner = getFullStringScanner(in);
            JSONObject json = new JSONObject(scanner.next());
            return configuration.getSchemaMediator().createOrUpdateUsingJsonObject(clazz, this, json, true);
        } catch (JSONException e) {
            throw new RealmException("Failed to read JSON", e);
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
    }
	

    private Scanner getFullStringScanner(InputStream in) {
        return new Scanner(in, "UTF-8").useDelimiter("\\A");
    }
	

    /**
     * Write a compacted copy of the Realm to the given destination File.
     * <p>
     * The destination file cannot already exist.
     * <p>
     * Note that if this is called from within a write transaction it writes the
     * current data, and not the data as it was when the last write transaction was committed.
     *
     * @param destination File to save the Realm to
     * @throws java.io.IOException if any write operation fails
     */
    
	

    /**
     * Write a compacted and encrypted copy of the Realm to the given destination File.
     * <p>
     * The destination file cannot already exist.
     * <p>
     * Note that if this is called from within a write transaction it writes the
     * current data, and not the data as it was when the last write transaction was committed.
     * <p>
     * @param destination File to save the Realm to
     * @throws java.io.IOException if any write operation fails
     * @throws RealmEncryptionNotSupportedException if the device doesn't support Realm encryption.
     */
    
	


    /**
     * Instantiates and adds a new object to the Realm.
     *
     * @param clazz The Class of the object to create
     * @return The new object
     * @throws RealmException An object could not be created
     */
    public <E extends RealmObject> E createObject(Class<E> clazz) {
        Table table = getTable(clazz);
        long rowIndex = table.addEmptyRow();
        return getByIndex(clazz, rowIndex);
    }
	

    /**
     * Creates a new object inside the Realm with the Primary key value initially set.
     * If the value violates the primary key constraint, no object will be added and a
     * {@link RealmException} will be thrown.
     *
     * @param clazz The Class of the object to create
     * @param primaryKeyValue Value for the primary key field.
     * @return The new object
     * @throws {@link RealmException} if object could not be created.
     */
    <E extends RealmObject> E createObject(Class<E> clazz, Object primaryKeyValue) {
        Table table = getTable(clazz);
        long rowIndex = table.addEmptyRowWithPrimaryKey(primaryKeyValue);
        return getByIndex(clazz, rowIndex);
    }
	

    void remove(Class<? extends RealmObject> clazz, long objectIndex) {
        getTable(clazz).moveLastOver(objectIndex);
    }
	

    <E extends RealmObject> E getByIndex(Class<E> clazz, long rowIndex) {
        Table table = getTable(clazz);
        UncheckedRow row = table.getUncheckedRowByIndex(rowIndex);
        E result = configuration.getSchemaMediator().newInstance(clazz);
        result.row = row;
        result.realm = this;
        return result;
    }
	

    
	

    /**
     * Copies a RealmObject to the Realm instance and returns the copy. Any further changes to the original RealmObject
     * will not be reflected in the Realm copy. This is a deep copy, so all referenced objects will be copied. Objects
     * already in this Realm will be ignored.
     *
     * @param object {@link io.realm.RealmObject} to copy to the Realm.
     * @return A managed RealmObject with its properties backed by the Realm.
     *
     * @throws java.lang.IllegalArgumentException if RealmObject is {@code null}.
     */
    public <E extends RealmObject> E copyToRealm(E object) {
        checkNotNullObject(object);
        return copyOrUpdate(object, false);
    }
	

    /**
     * Updates an existing RealmObject that is identified by the same {@link io.realm.annotations.PrimaryKey} or create
     * a new copy if no existing object could be found. This is a deep copy or update, so all referenced objects will be
     * either copied or updated.
     *
     * @param object    {@link io.realm.RealmObject} to copy or update.
     * @return The new or updated RealmObject with all its properties backed by the Realm.
     *
     * @throws java.lang.IllegalArgumentException if RealmObject is {@code null} or doesn't have a Primary key defined.
     * @see #copyToRealm(RealmObject)
     */
    public <E extends RealmObject> E copyToRealmOrUpdate(E object) {
        checkNotNullObject(object);
        checkHasPrimaryKey(object.getClass());
        return copyOrUpdate(object, true);
    }
	

    /**
     * Copies a collection of RealmObjects to the Realm instance and returns their copy. Any further changes
     * to the original RealmObjects will not be reflected in the Realm copies. This is a deep copy, so all referenced
     * objects will be copied. Objects already in this Realm will be ignored.
     *
     * @param objects RealmObjects to copy to the Realm.
     * @return A list of the the converted RealmObjects that all has their properties managed by the Realm.
     *
     * @throws io.realm.exceptions.RealmException if any of the objects has already been added to Realm.
     * @throws java.lang.IllegalArgumentException if any of the elements in the input collection is {@code null}.
     */
    public <E extends RealmObject> List<E> copyToRealm(Iterable<E> objects) {
        if (objects == null) {
            return new ArrayList<E>();
        }

        ArrayList<E> realmObjects = new ArrayList<E>();
        for (E object : objects) {
            realmObjects.add(copyToRealm(object));
        }

        return realmObjects;
    }
	

    /**
     * Updates a list of existing RealmObjects that is identified by their {@link io.realm.annotations.PrimaryKey} or create a
     * new copy if no existing object could be found. This is a deep copy or update, so all referenced objects will be
     * either copied or updated.
     *
     * @param objects   List of objects to update or copy into Realm.
     * @return A list of all the new or updated RealmObjects.
     *
     * @throws java.lang.IllegalArgumentException if RealmObject is {@code null} or doesn't have a Primary key defined.
     * @see #copyToRealm(Iterable)
     */
    public <E extends RealmObject> List<E> copyToRealmOrUpdate(Iterable<E> objects) {
        if (objects == null) {
            return new ArrayList<E>();
        }

        ArrayList<E> realmObjects = new ArrayList<E>();
        for (E object : objects) {
            realmObjects.add(copyToRealmOrUpdate(object));
        }

        return realmObjects;
    }
	

    boolean contains(Class<? extends RealmObject> clazz) {
        return configuration.getSchemaMediator().getModelClasses().contains(clazz);
    }
	

    /**
     * Returns a typed RealmQuery, which can be used to query for specific objects of this type
     *
     * @param clazz The class of the object which is to be queried for
     * @return A typed RealmQuery, which can be used to query for specific objects of this type
     * @throws java.lang.RuntimeException Any other error
     * @see io.realm.RealmQuery
     */
    public <E extends RealmObject> RealmQuery<E> where(Class<E> clazz) {
        checkIfValid();
        return new RealmQuery<E>(this, clazz);
    }
	

    /**
     * Get all objects of a specific Class. If no objects exist, the returned RealmResults will not
     * be null. The RealmResults.size() to check the number of objects instead.
     *
     * @param clazz the Class to get objects of
     * @return A RealmResult list containing the objects
     * @throws java.lang.RuntimeException Any other error
     * @see io.realm.RealmResults
     */
    public <E extends RealmObject> RealmResults<E> allObjects(Class<E> clazz) {
        return where(clazz).findAll();
    }
	

    /**
     * Get all objects of a specific Class sorted by a field.  If no objects exist, the returned
     * RealmResults will not be null. The RealmResults.size() to check the number of objects instead.
     *
     * @param clazz the Class to get objects of.
     * @param fieldName the field name to sort by.
     * @param sortAscending sort ascending if SORT_ORDER_ASCENDING, sort descending if SORT_ORDER_DESCENDING.
     * @return A sorted RealmResults containing the objects.
     * @throws java.lang.IllegalArgumentException if field name does not exist.
     */
    public <E extends RealmObject> RealmResults<E> allObjectsSorted(Class<E> clazz, String fieldName,
                                                                    boolean sortAscending) {
        checkIfValid();
        Table table = getTable(clazz);
        TableView.Order order = sortAscending ? TableView.Order.ascending : TableView.Order.descending;
        long columnIndex = columnIndices.getColumnIndex(clazz, fieldName);
        if (columnIndex < 0) {
            throw new IllegalArgumentException(String.format("Field name '%s' does not exist.", fieldName));
        }

        TableView tableView = table.getSortedView(columnIndex, order);
        return new RealmResults<E>(this, tableView, clazz);
    }
	

    // Notifications

    /**
     * Add a change listener to the Realm.
     * <p>
     * The listeners will be executed:
     * <ul>
     *     <li>Immediately if a change was committed by the local thread</li>
     *     <li>On every loop of a Handler thread if changes were committed by another thread</li>
     *     <li>On every call to {@link io.realm.Realm#refresh()}</li>
     * </ul>
     *
     * @param listener the change listener
     * @see io.realm.RealmChangeListener
     */
    
	

    /**
     * Remove the specified change listener
     *
     * @param listener the change listener to be removed
     * @see io.realm.RealmChangeListener
     */
    
	

    /**
     * Remove all user-defined change listeners
     *
     * @see io.realm.RealmChangeListener
     */
    
	


    /**
     * Get all objects of a specific class sorted by two specific field names.  If no objects exist,
     * the returned RealmResults will not be null. The RealmResults.size() to check the number of
     * objects instead.
     *
     * @param clazz the class ti get objects of.
     * @param fieldName1 first field name to sort by.
     * @param sortAscending1 sort order for first field.
     * @param fieldName2 second field name to sort by.
     * @param sortAscending2 sort order for second field.
     * @return A sorted RealmResults containing the objects.
     * @throws java.lang.IllegalArgumentException if a field name does not exist.
     */
    public <E extends RealmObject> RealmResults<E> allObjectsSorted(Class<E> clazz, String fieldName1,
                                                               boolean sortAscending1, String fieldName2,
                                                                    boolean sortAscending2) {
        return allObjectsSorted(clazz, new String[]{fieldName1, fieldName2}, new boolean[]{sortAscending1,
                sortAscending2});
    }
	

    /**
     * Get all objects of a specific class sorted by two specific field names.  If no objects exist,
     * the returned RealmResults will not be null. The RealmResults.size() to check the number of
     * objects instead.
     *
     * @param clazz the class ti get objects of.
     * @param fieldName1 first field name to sort by.
     * @param sortAscending1 sort order for first field.
     * @param fieldName2 second field name to sort by.
     * @param sortAscending2 sort order for second field.
     * @param fieldName3 third field name to sort by.
     * @param sortAscending3 sort order for third field.
     * @return A sorted RealmResults containing the objects.
     * @throws java.lang.IllegalArgumentException if a field name does not exist.
     */
    public <E extends RealmObject> RealmResults<E> allObjectsSorted(Class<E> clazz, String fieldName1,
                                                                    boolean sortAscending1,
                                                                    String fieldName2, boolean sortAscending2,
                                                                    String fieldName3, boolean sortAscending3) {
        return allObjectsSorted(clazz, new String[]{fieldName1, fieldName2, fieldName3},
                new boolean[]{sortAscending1, sortAscending2, sortAscending3});
    }
	

    /**
     * Get all objects of a specific Class sorted by a field.  If no objects exist, the returned
     * RealmResults will not be null. The RealmResults.size() to check the number of objects instead.
     *
     * @param clazz the Class to get objects of.
     * @param fieldName the field name to sort by.
     * @param sortAscending sort ascending if SORT_ORDER_ASCENDING, sort descending if SORT_ORDER_DESCENDING.
     * @return A sorted RealmResults containing the objects.
     * @throws java.lang.IllegalArgumentException if field name does not exist.
     */
    @SuppressWarnings("unchecked")
    public <E extends RealmObject> RealmResults<E> allObjectsSorted(Class<E> clazz, String fieldNames[],
                                                                    boolean sortAscending[]) {
        if (fieldNames == null) {
            throw new IllegalArgumentException("fieldNames must be provided.");
        } else if (sortAscending == null) {
            throw new IllegalArgumentException("sortAscending must be provided.");
        }

        // Convert field names to column indices
        Table table = this.getTable(clazz);
        long columnIndices[] = new long[fieldNames.length];
        for (int i = 0; i < fieldNames.length; i++) {
            String fieldName = fieldNames[i];
            long columnIndex = table.getColumnIndex(fieldName);
            if (columnIndex == -1) {
                throw new IllegalArgumentException(String.format("Field name '%s' does not exist.", fieldName));
            }
            columnIndices[i] = columnIndex;
        }

        // Perform sort
        TableView tableView = table.getSortedView(columnIndices, sortAscending);
        return new RealmResults(this, tableView, clazz);
    }
	

    /**
     * Return change listeners
     * For internal testing purpose only
     *
     * @return changeListeners list of this realm instance
     */
    protected List<WeakReference<RealmChangeListener>> getChangeListeners() {
        return changeListeners;
    }
	

    
	

    @SuppressWarnings("UnusedDeclaration")
    boolean hasChanged() {
        return sharedGroupManager.hasChanged();
    }
	

    /**
     * Transactions
     */

    /**
     * Refresh the Realm instance and all the RealmResults and RealmObjects instances coming from it.
     * It also calls the listeners associated to the Realm instance.
     */
    
	

    /**
     * Starts a write transaction, this must be closed with {@link io.realm.Realm#commitTransaction()}
     * or aborted by {@link io.realm.Realm#cancelTransaction()}. Write transactions are used to
     * atomically create, update and delete objects within a realm.
     * <br>
     * Before beginning the write transaction, {@link io.realm.Realm#beginTransaction()} updates the
     * realm in the case of pending updates from other threads.
     * <br>
     * Notice: it is not possible to nest write transactions. If you start a write
     * transaction within a write transaction an exception is thrown.
     * <br>
     * @throws java.lang.IllegalStateException If already in a write transaction or incorrect thread.
     *
     */
    
	

    /**
     * All changes since {@link io.realm.Realm#beginTransaction()} are persisted to disk and the
     * Realm reverts back to being read-only. An event is sent to notify all other realm instances
     * that a change has occurred. When the event is received, the other Realms will get their
     * objects and {@link io.realm.RealmResults} updated to reflect
     * the changes from this commit.
     *
     * @throws java.lang.IllegalStateException If the write transaction is in an invalid state or incorrect thread.
     */
    
	

    /**
     * Revert all writes (created, updated, or deleted objects) made in the current write
     * transaction and end the transaction.
     * <br>
     * The Realm reverts back to read-only.
     * <br>
     * Calling this when not in a write transaction will throw an exception.
     *
     * @throws java.lang.IllegalStateException    If the write transaction is an invalid state,
     *                                             not in a write transaction or incorrect thread.
     */
    
	

    /**
     * Executes a given transaction on the Realm. {@link #beginTransaction()} and
     * {@link #commitTransaction()} will be called automatically. If any exception is thrown
     * during the transaction {@link #cancelTransaction()} will be called instead of {@link #commitTransaction()}.
     *
     * @param transaction {@link io.realm.Realm.Transaction} to execute.
     * @throws RealmException if any error happened during the transaction.
     */
    public void executeTransaction(Transaction transaction) {
        if (transaction == null)
            throw new IllegalArgumentException("transaction should not be null");

        beginTransaction();
        try {
            transaction.execute(this);
            commitTransaction();
        } catch (RuntimeException e) {
            cancelTransaction();
            throw new RealmException("Error during transaction.", e);
        } catch (Error e) {
            cancelTransaction();
            throw e;
        }
    }
	

    /**
     * Similar to {@link #executeTransaction(Transaction)} but runs asynchronously from a worker thread
     * @param transaction {@link io.realm.Realm.Transaction} to execute.
     * @param callback optional, to receive the result of this query
     * @return A {@link io.realm.Realm.Request} representing a cancellable task
     */
    public Request executeTransaction(final Transaction transaction, final Transaction.Callback callback) {
        if (transaction == null)
            throw new IllegalArgumentException("transaction should not be null");

        if (callback == null)
            throw new IllegalArgumentException("callback should not be null");

        // will use the Looper of the caller thread to post the result
        final Handler handler = new Handler();

        // We need to use the same configuration to open a background SharedGroup (i.e Realm)
        // to perform the transaction
        final RealmConfiguration realmConfiguration = getConfiguration();

        final Future<?> pendingQuery = asyncQueryExecutor.submit(new Runnable() {
            @Override
            public void run() {
                if (!Thread.currentThread().isInterrupted()) {
                    Realm bgRealm = Realm.getInstance(realmConfiguration);
                    bgRealm.beginTransaction();
                    try {
                        transaction.execute(bgRealm);

                        if (!Thread.currentThread().isInterrupted()) {
                            bgRealm.commitTransaction();
                            if (callback != null
                                    && !Thread.currentThread().isInterrupted()
                                    && handler.getLooper().getThread().isAlive()) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        callback.onSuccess();
                                    }
                                });
                            }
                        } else {
                            bgRealm.cancelTransaction();
                        }

                    } catch (final Exception e) {
                        bgRealm.cancelTransaction();
                        if (callback != null
                                && !Thread.currentThread().isInterrupted()
                                && handler.getLooper().getThread().isAlive()) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onError(e);
                                }
                            });
                        }
                    } finally {
                        bgRealm.close();
                    }
                }
            }
        });

        return new Request(pendingQuery);
    }
	

    /**
     * Remove all objects of the specified class.
     *
     * @param clazz The class which objects should be removed
     * @throws java.lang.RuntimeException Any other error
     */
    public void clear(Class<? extends RealmObject> clazz) {
        getTable(clazz).clear();
    }
	

    // package protected so unit tests can access it

    // Returns the Handler for this Realm on the calling thread
    Handler getHandler() {
        return handler;
    }

	

    void setHandler (Handler handler) {
        // remove the old one
        handlers.remove(this.handler);
        handlers.put(handler, configuration.getPath());
        this.handler = handler;
    }
	

    
	

    // package protected so unit tests can access it
    
	

    @SuppressWarnings("unchecked")
    private <E extends RealmObject> Class<? extends RealmObject> getRealmClassFromObject(E object) {
        if (object.realm != null) {
            // This is already a proxy object, get superclass instead
            // INVARIANT: We don't support subclasses yet so super class is always correct type
            return (Class<? extends RealmObject>) object.getClass().getSuperclass();
        } else {
            return object.getClass();
        }
    }
	

    @SuppressWarnings("unchecked")
    private <E extends RealmObject> E copyOrUpdate(E object, boolean update) {
        return configuration.getSchemaMediator().copyOrUpdate(this, object, update, new HashMap<RealmObject, RealmObjectProxy>());
    }
	

    private <E extends RealmObject> void checkNotNullObject(E object) {
        if (object == null) {
            throw new IllegalArgumentException("Null objects cannot be copied into Realm.");
        }
    }
	

    private <E extends RealmObject> void checkHasPrimaryKey(E object) {
        Class<? extends RealmObject> objectClass = object.getClass();
        if (!getTable(objectClass).hasPrimaryKey()) {
            throw new IllegalArgumentException("RealmObject has no @PrimaryKey defined: " + objectClass.getSimpleName());
        }
    }
	

    private void checkHasPrimaryKey(Class<? extends RealmObject> clazz) {
        if (!getTable(clazz).hasPrimaryKey()) {
            throw new IllegalArgumentException("A RealmObject with no @PrimaryKey cannot be updated: " + clazz.toString());
        }
    }
	

    protected void checkIfValid() {
        super.checkIfValid();
    }
	

    @Override
    protected Map<RealmConfiguration, Integer> getLocalReferenceCount() {
        return referenceCount.get();
    }
	

    @Override
    protected void lastLocalInstanceClosed() {
        validatedRealmFiles.remove(configuration.getPath());
        realmsCache.get().remove(configuration);
    }
	

    /**
     * Manually trigger the migration associated with a given RealmConfiguration. If Realm is already at the
     * latest version, nothing will happen.
     * @param configuration
     */
    public static void migrateRealm(RealmConfiguration configuration) {
        migrateRealm(configuration, null);
    }
	

    /**
     * Manually trigger a migration on a RealmMigration.
     *
     * @param configuration {@link RealmConfiguration}
     * @param migration {@link RealmMigration} to run on the Realm. This will override any migration set on the
     * configuration.
     */
    public static void migrateRealm(RealmConfiguration configuration, RealmMigration migration) {
        BaseRealm.migrateRealm(configuration, migration, new MigrationCallback() {

            @Override
            public BaseRealm getRealm(RealmConfiguration configuration) {
                return Realm.createAndValidate(configuration, false, Looper.myLooper() != null);
        }

            @Override
            public void migrationComplete() {
                realmsCache.remove();
            }
        });
    }
	

    /**
     * Delete the Realm file specified by the given {@link RealmConfiguration} from the filesystem.
     * The Realm must be unused and closed before calling this method.
     *
     * @param configuration A {@link RealmConfiguration}
     * @return false if a file could not be deleted. The failing file will be logged.
     *
     * @throws java.lang.IllegalStateException if trying to delete a Realm that is already open.
     */
    public static boolean deleteRealm(RealmConfiguration configuration) {
        return BaseRealm.deleteRealm(configuration);
    }
	

    /**
     * Compact a Realm file. A Realm file usually contain free/unused space.
     * This method removes this free space and the file size is thereby reduced.
     * Objects within the Realm files are untouched.
     * <p>
     * The file must be closed before this method is called.<br>
     * The file system should have free space for at least a copy of the Realm file.<br>
     * The Realm file is left untouched if any file operation fails.<br>
     *
     * @param configuration a {@link RealmConfiguration} pointing to a Realm file.
     * @return true if successful, false if any file operation failed
     *
     * @throws java.lang.IllegalStateException if trying to compact a Realm that is already open.
     */
    public static boolean compactRealm(RealmConfiguration configuration) {
        return BaseRealm.compactRealm(configuration);
    }
	

    /**
     * Returns the canonical path to where this Realm is persisted on disk.
     *
     * @return The canonical path to the Realm file.
     * @see File#getCanonicalPath()
     */
    
	

    /**
     * Returns the {@link RealmConfiguration} for this Realm.
     * @return {@link RealmConfiguration} for this Realm.
     */
    
	

    // Get the canonical path for a given file
    static String getCanonicalPath(File realmFile) {
        try {
            return realmFile.getCanonicalPath();
        } catch (IOException e) {
            throw new RealmException("Could not resolve the canonical path to the Realm file: " + realmFile.getAbsolutePath());
        }
    }
	

    // Return all handlers registered for this Realm
    static Map<Handler, String> getHandlers() {
        return handlers;
    }
	

    // Public because of migrations
    public Table getTable(Class<? extends RealmObject> clazz) {
        Table table = classToTable.get(clazz);
        if (table == null) {
            clazz = Util.getOriginalModelClass(clazz);
            table = sharedGroupManager.getTable(configuration.getSchemaMediator().getTableName(clazz));
            classToTable.put(clazz, table);
        }
        return table;
    }
	

    /**
     * Returns the default Realm module. This module contains all Realm classes in the current project, but not
     * those from library or project dependencies. Realm classes in these should be exposed using their own module.
     *
     * @return The default Realm module or null if no default module exists.
     * @see io.realm.RealmConfiguration.Builder#setModules(Object, Object...)
     */
    public static Object getDefaultModule() {
        String moduleName = "io.realm.DefaultRealmModule";
        Class<?> clazz;
        try {
            clazz = Class.forName(moduleName);
            Constructor<?> constructor = clazz.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ClassNotFoundException e) {
            return null;
        } catch (InvocationTargetException e) {
            throw new RealmException("Could not create an instance of " + moduleName, e);
        } catch (InstantiationException e) {
            throw new RealmException("Could not create an instance of " + moduleName, e);
        } catch (IllegalAccessException e) {
            throw new RealmException("Could not create an instance of " + moduleName, e);
        }
    }
	

    /**
     * Encapsulates a Realm transaction.
     * <p>
     * Using this class will automatically handle {@link #beginTransaction()} and {@link #commitTransaction()}
     * If any exception is thrown during the transaction {@link #cancelTransaction()} will be called
     * instead of {@link #commitTransaction()}.
     */
    public  interface  Transaction {
		
        void execute(Realm realm);
		

        /**
         * Callback invoked after a {#Transaction} call, to notify the caller thread
         */
         

        /**
         * Callback invoked after a {#Transaction} call, to notify the caller thread
         */
        class  Callback {
			
            public void onSuccess() {}
			
            public void onError(Exception e) {}

		}

	}
	

    /**
     * Represents a pending asynchronous Realm query.
     * <p/>
     * Users are responsible of maintaining a reference to {@code Request} in order
     * to call #cancel in case of a configuration change for example (to avoid memory leak, as the
     * query will post the result to the caller's thread callback)
     */
    public static  class  Request {
		
        private final Future<?> pendingQuery;
		
        private volatile boolean isCancelled = false;
		

        public Request(Future<?> pendingQuery) {
            this.pendingQuery = pendingQuery;
        }
		

        /**
         * Attempts to cancel execution of this query (if it hasn't already completed or previously cancelled)
         */
        public void cancel() {
            pendingQuery.cancel(true);
            isCancelled = true;

            // cancelled tasks are never executed, but may
            // accumulate in work queues, which may causes a memory leak
            // if the task hold references (to an enclosing class for example)
            // we can use purge() but one caveat applies: if a second thread attempts to add
            // something to the pool (suing the execute() method) at the same time the
            // first thread is attempting to purge the queue the attempt to purge
            // the queue fails and the cancelled object remain in the queue.
            // A better way to cancel objects with thread pools is to use the remove()
            asyncQueryExecutor.getQueue().remove(pendingQuery);
        }
		

        /**
         * Whether an attempt to cancel the query was performed
         *
         * @return {@code true} if {@link #cancel()} has already been called, {@code false} otherwise
         */
        public boolean isCancelled() {
            return isCancelled;
        }

	}

}
