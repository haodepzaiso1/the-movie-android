# 📊 PHÂN TÍCH LUỒNG DỮ LIỆU - HILT MVVM COMPOSE MOVIE APP

## 📝 Tóm tắt

Project này là một Android App hiển thị danh sách phim, TV series, và celebrity. Dữ liệu đến từ **TMDB API** (TheMovieDatabase) và được lưu trữ locally trong **Room Database** (cho các phim yêu thích). App sử dụng kiến trúc **MVVM** kết hợp với **Hilt** cho dependency injection và **Jetpack Compose** cho UI.

---

## 🏗️ KIẾN TRÚC TỔNG QUAN

```
┌──────────────────────────────────────────────────────────────┐
│                    JETPACK COMPOSE UI LAYER                   │
│  (NowPlayingMovie.kt, MovieDetail.kt, PopularMovie.kt, etc)   │
└──────────────────────────────────────────┬────────────────────┘
                                           │
                                      ▼    │
┌──────────────────────────────────────────────────────────────┐
│                   VIEWMODEL LAYER (@HiltViewModel)             │
│  NowPlayingMovieViewModel, MovieDetailViewModel,               │
│  TopRatedMovieViewModel, PopularTvSeriesViewModel, etc         │
│  - Quản lý UI state                                            │
│  - Gọi Repository để lấy dữ liệu                               │
│  - Caching PagingData trong viewModelScope                     │
└──────────────────────────┬───────────────────────────────────┘
                           │
                      ▼    │
┌─────────────────────────────────────────────────────────────┐
│                 REPOSITORY LAYER (Data Abstraction)          │
│                                                               │
│  ┌─────────────────────────────────────┐                    │
│  │  Remote Repositories (Retrofit API) │                    │
│  ├─────────────────────────────────────┤                    │
│  │ MovieRepository     (TMDB API)     │                    │
│  │ TvSeriesRepository  (TMDB API)     │                    │
│  │ CelebrityRepository (TMDB API)     │                    │
│  │ ArtistRepository    (TMDB API)     │                    │
│  └─────────────────────────────────────┘                    │
│                                                               │
│  ┌─────────────────────────────────────┐                    │
│  │ Local Repositories (Room Database)  │                    │
│  ├─────────────────────────────────────┤                    │
│  │ LocalMovieRepository                │                    │
│  │ LocalTvSeriesRepository             │                    │
│  └─────────────────────────────────────┘                    │
└──┬───────────────────┬──────────────────────────────────────┘
   │                   │
   │                   └─────────────────┐
   │                                     ▼
   │     ┌───────────────────────────────────────────┐
   │     │  PAGING DATASOURCES (for lazy loading)    │
   │     ├───────────────────────────────────────────┤
   │     │ NowPlayingMoviePagingDataSource            │
   │     │ PopularMoviePagingDataSource               │
   │     │ TopRatedMoviePagingDataSource              │
   │     │ UpcomingMoviePagingDataSource              │
   │     │ PopularCelebritiesPagingDataSource         │
   │     │ GenrePagingDataSource                      │
   │     │ (Tất cả đều kế thừa PagingSource)          │
   │     └──────────────────┬──────────────────────┘
   │                        │
   ▼                        ▼
┌─────────────────────────────────────────────────────────────┐
│        DATA SOURCE LAYER (Network + Database)                │
│                                                               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              ApiService (Retrofit)                   │  │
│  │  - Giao tiếp với TMDB API                            │  │
│  │  - 27 API endpoints (movies, tv, people, etc)        │  │
│  │  - Trả về data models (MovieItem, TvSeriesItem)      │  │
│  └──────────────────────┬───────────────────────────────┘  │
│                         │                                    │
│                         ▼                                    │
│  ┌──────────────────────────────────────────────────────┐  │
│  │    NETWORK LAYER (Retrofit + OkHttp)                 │  │
│  ├──────────────────────────────────────────────────────┤  │
│  │ ApiKeyInterceptor  - Inject API key tự động         │  │
│  │ HttpLoggingInterceptor - Log API requests/responses  │  │
│  │ OkHttpClient - HTTP client backend                   │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         LOCAL DATABASE (Room)                        │  │
│  ├──────────────────────────────────────────────────────┤  │
│  │ MovieWorldDatabase                                   │  │
│  │  ├─ FavoriteMovieDao  → MovieDetail                  │  │
│  │  └─ FavoriteTvSeriesDao → TvSeriesDetail             │  │
│  │                                                       │  │
│  │ TypeConverters:                                      │  │
│  │  - MovieTypeConverter (JSON ↔ MovieDetail)           │  │
│  │  - TvSeriesTypeConverter (JSON ↔ TvSeriesDetail)     │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────┬──────────────────────────────────────────────┘
              │
         ▼    │
    ┌────────────────────────────┐
    │  EXTERNAL: TMDB API        │
    │  & Device Storage (SQLite) │
    └────────────────────────────┘
```

---

## 📱 CHI TIẾT LUỒNG DỮ LIỆU - SỬ DỤNG TRƯỜNG HỢP THỰC TẾ

### **CASE 1: Xem danh sách phim "Now Playing"**

**Điểm bắt đầu:** User mở app và vào tab "Now Playing"

#### **Tầng UI (Composable)**
```
File: NowPlayingMovie.kt
  └─ @Composable NowPlayingMovie()
     ├─ Nhận viewModel: NowPlayingMovieViewModel
     ├─ Collect nowPlayingMovies Flow từ ViewModel
     └─ Render LazyColumn với phim danh sách (pagination)
```

#### **Tầng ViewModel**
```
File: NowPlayingMovieViewModel.kt
Annotation: @HiltViewModel (Hilt tự inject)

Class: NowPlayingMovieViewModel
├─ Constructor Injection:
│  └─ repo: MovieRepository
│
├─ Observable State:
│  ├─ _uiState: MutableStateFlow<UiState>
│  │  ├─ isLoading: Boolean
│  │  └─ errorMessage: String?
│  │
│  ├─ selectedGenre: mutableStateOf<Genre>
│  ├─ filterData: MutableStateFlow<GenreId?>
│  │
│  └─ nowPlayingMovies: Flow<PagingData<MovieItem>>
│     └─ Được tạo bằng flatMapLatest:
│        ┌──────────────────────────────────────┐
│        │ filterData.flatMapLatest { genreId   │
│        │   repo.nowPlayingMoviePagingDataSource│
│        │        (genreId?.genreId)             │
│        │ }.cachedIn(viewModelScope)            │
│        └──────────────────────────────────────┘
│
├─ Methods:
│  ├─ onGenreSelected(genre: Genre?)
│  │  └─ Cập nhật filterData → trigger flatMapLatest
│  │
│  └─ updateLoadState(loadState: CombinedLoadStates)
│     └─ Xác định trạng thái loading/error
│
└─ Lifecycle: Tồn tại khi Activity còn sống
```

#### **Tầng Repository**
```
File: MovieRepositoryImpl.kt

Interface: MovieRepository (định nghĩa contract)
Implementation: MovieRepositoryImpl
  └─ Constructor Injection:
     └─ apiService: ApiService

Method: nowPlayingMoviePagingDataSource(genreId: String?): Flow<PagingData<MovieItem>>
  └─ Tạo dùng Pager:
     ┌──────────────────────────────────────────────┐
     │ Pager(                                       │
     │   pagingSourceFactory = {                    │
     │     NowPlayingMoviePagingDataSource(          │
     │       apiService, genreId                    │
     │     )                                        │
     │   },                                         │
     │   config = PagingConfig(pageSize = 20)       │
     │ ).flow                                       │
     └──────────────────────────────────────────────┘
     
     - Mỗi trang = 20 items
     - Lazy load trang khi cuộn đến cuối
```

#### **Tầng Paging DataSource**
```
File: NowPlayingMoviePagingDataSource.kt

Class: NowPlayingMoviePagingDataSource : PagingSource<Int, MovieItem>
  ├─ Constructor:
  │  ├─ apiService: ApiService
  │  └─ genreId: String?
  │
  └─ Override fun load(params: LoadParams<Int>): LoadResult<Int, MovieItem>
     ├─ val page = params.key ?: 1
     ├─ TRỊ GỌI API:
     │  └─ apiService.nowPlayingMovies(page, genreId)
     │     ├─ Query: ?page=1&with_genres=28 (nếu genreId=28)
     │     └─ Returns: BaseModel<MovieItem>
     │
     ├─ Xử lý response:
     │  └─ LoadResult.Page(
     │       data = response.results,
     │       prevKey = if (page > 1) page - 1 else null,
     │       nextKey = if (response.results.isNotEmpty()) page + 1 else null
     │     )
     │
     └─ Error handling:
        └─ LoadResult.Error(exception)
```

#### **Tầng Network - API Service**
```
File: ApiService.kt

Interface: ApiService (Retrofit)
  └─ @GET("movie/now_playing")
     suspend fun nowPlayingMovies(
       @Query("page") page: Int,
       @Query("with_genres") genreId: String?
     ): BaseModel<MovieItem>
     
     Network Flow:
     1. Retrofit call = HTTP GET
     2. URL: https://api.themoviedb.org/3/movie/now_playing
     3. Query Parameters:
        - page: 1 (hoặc trang tiếp theo)
        - with_genres: 28 (nếu user filter)
        - api_key: [được inject bằng ApiKeyInterceptor]
     4. Response: JSON từ TMDB
     5. GSON convert → BaseModel<MovieItem>
```

#### **HTTP Interceptors**
```
ApiKeyInterceptor.kt:
  ├─ Tự động thêm "api_key" query parameter vào mỗi request
  ├─ Lấy API key từ BuildConfig.API_KEY
  └─ Mọi API call đều có api_key

HttpLoggingInterceptor.kt:
  ├─ Level: BODY
  ├─ Log full request/response (debug)
  └─ In ra Logcat
```

#### **Dữ liệu được trả về**
```
JSON Response từ TMDB:
{
  "page": 1,
  "results": [
    {
      "id": 1,
      "title": "Movie Title",
      "poster_path": "/path/to/poster.jpg",
      "backdrop_path": "/path/to/backdrop.jpg",
      "overview": "Movie description...",
      "release_date": "2024-01-01",
      "vote_average": 7.5
    },
    ...
  ],
  "total_pages": 50,
  "total_results": 1000
}

↓ GSON Deserialization ↓

MovieItem (Data Model):
  ├─ id: Int
  ├─ title: String
  ├─ posterPath: String (JSON field: poster_path)
  ├─ backdropPath: String (JSON field: backdrop_path)
  ├─ overview: String
  ├─ releaseDate: String (JSON field: release_date)
  └─ voteAverage: Double (JSON field: vote_average)

BaseModel<MovieItem>:
  ├─ page: Int
  ├─ results: List<MovieItem>
  ├─ totalPages: Int
  └─ totalResults: Int
```

#### **Hiển thị trên UI**
```
NowPlayingMovie.kt:
  ├─ Receive nowPlayingMovies: Flow<PagingData<MovieItem>> từ ViewModel
  ├─ collectAsLazyPagingItems() → LazyPagingItems<MovieItem>
  ├─ LazyColumn { items(pagingItems) { item ->
  │    MovieItem(item)  // Render từng phim
  │  }
  └─ Pagination tự động: cuộn xuống = load page tiếp theo
```

---

### **CASE 2: Xem chi tiết phim (Movie Detail)**

**Điểm bắt đầu:** User click vào một phim trong danh sách

#### **Tầng UI → ViewModel → Repository → API**
```
MovieDetail.kt (@Composable)
  │
  ├─ LaunchedEffect(movieId) {
  │  └─ Trigger load data khi screen open
  │
  └─ val viewModel: MovieDetailViewModel = hiltViewModel()
     │
     ├─ ViewModel:
     │  ├─ uiState: StateFlow<MovieDetailUiState>
     │  └─ init { loadMovieDetail(movieId) }
     │
     └─ Repository Methods:
        ├─ movieDetail(movieId: Int): Flow<DataState<MovieDetail>>
        │  └─ apiService.movieDetail(movieId)
        │     └─ @GET("movie/{movieId}")
        │        → TMDB: /movie/550 (Fight Club)
        │           → Trả MovieDetail object
        │
        ├─ recommendedMovie(movieId: Int): Flow<DataState<List<MovieItem>>>
        │  └─ apiService.recommendedMovie(movieId)
        │     └─ @GET("movie/{movieId}/recommendations")
        │
        └─ movieCredit(movieId: Int): Flow<DataState<Artist>>
           └─ apiService.movieCredit(movieId)
              └─ @GET("movie/{movieId}/credits")
```

#### **Data Models - Movie Detail**
```
File: moviedetail/MovieDetail.kt (Entity + DAO)

@Entity
@TypeConverters(MovieTypeConverter::class)
data class MovieDetail(
  @PrimaryKey
  val id: Int,
  val title: String,
  val overview: String,
  val posterPath: String?,
  val backdropPath: String?,
  val releaseDate: String?,
  val runtime: Int,
  val budget: Long,
  val revenue: Long,
  val voteAverage: Double,
  val voteCount: Int,
  val homepage: String?,
  val status: String,
  val genres: List<Genre>,  // Nested object (converted by TypeConverter)
  val productionCompanies: List<ProductionCompany>,
  val productionCountries: List<ProductionCountry>,
  val spokenLanguages: List<SpokenLanguage>
)

Nested Classes:
├─ Genre
│  ├─ id: Int
│  └─ name: String
├─ ProductionCompany
│  ├─ id: Int
│  └─ name: String
├─ ProductionCountry
│  ├─ iso3166_1: String
│  └─ name: String
└─ SpokenLanguage
   ├─ english_name: String
   └─ iso639_1: String
```

#### **Local Database - Room**
```
File: MovieWorldDataBase.kt

@Database(
  version = 1,
  entities = [MovieDetail::class, TvSeriesDetail::class],
  exportSchema = false
)
@TypeConverters(MovieTypeConverter::class, TvSeriesTypeConverter::class)
abstract class MovieDatabase : RoomDatabase() {
  abstract fun getFavoriteMovieDetailDao(): FavoriteMovieDao
  abstract fun getFavoriteTvSeriesDao(): FavoriteTvSeriesDao
}

Các DAOs:

FavoriteMovieDao:
  ├─ @Insert(onConflict = REPLACE)
  │  suspend fun insertMovie(movieDetail: MovieDetail)
  │
  ├─ @Query("SELECT * FROM MovieDetail")
  │  suspend fun getAllMovieDetails(): List<MovieDetail>
  │
  ├─ @Query("SELECT * FROM MovieDetail WHERE id = :id")
  │  suspend fun getMovieById(id: Int): MovieDetail?
  │
  └─ @Query("DELETE FROM MovieDetail WHERE id = :movieId")
     suspend fun deleteMovieDetailById(movieId: Int)

LocalStorage:
  └─ SQLite Database (~/Hilt_test)
```

#### **Yêu thích (Favorites) - Luồng lưu trữ**
```
User Action: Click heart icon to add to favorites

MovieDetailViewModel:
  └─ addToFavorite(movie: MovieDetail)
     ├─ Gọi: localMovieRepository.addMovie(movie)
     │
     └─ LocalMovieRepository:
        └─ LocalMovieRepositoryImpl:
           └─ movieDao.insertMovie(movieDetail)
              └─ Room Database INSERT
                 ├─ Tạo mới hoặc UPDATE nếu đã tồn tại
                 ├─ TypeConverter: MovieDetail → JSON string
                 └─ Lưu vào SQLite

Lần sau User vào "Favorites" tab:

FavoriteMovieScreen.kt:
  └─ FavoriteMovieViewModel:
     └─ val favoriteMovies: StateFlow<List<MovieDetail>> =
        localMovieRepository.favoriteMovies()
          .map { it }
          .stateIn(viewModelScope, ...)
```

---

### **CASE 3: Tìm kiếm phim**

```
SearchScreen
  └─ User nhập queryString = "Avatar"
     │
     └─ ViewModel:
        ├─ searchQuery: MutableStateFlow<String> = "Avatar"
        │
        └─ searchResults: Flow<DataState<SearchBaseModel>> =
           searchQuery.flatMapLatest { query ->
             repo.movieSearch(query)
           }
        
        Repository:
        └─ movieSearch(searchKey: String): Flow<DataState<SearchBaseModel>>
           ├─ safeApiCall {
           │   apiService.searchMovie(searchKey)
           │ }
           │
           └─ ApiService:
              └─ @GET("search/movie")
                 suspend fun searchMovie(@Query("query") query: String): SearchBaseModel
                 
                 Network Request:
                 URL: /search/movie?query=Avatar&api_key=...
                 
                 Response:
                 SearchBaseModel:
                   └─ results: List<MovieItem>
                      └─ Chỉ có MovieItem cơ bản (không full MovieDetail)

UI:
  └─ Render searchResults.collectAsLazyPagingItems()
     └─ Hiển thị danh sách phim tìm được
```

---

### **CASE 4: Xem Celebrity/Artist**

```
PopularCelebrities.kt
  │
  └─ PopularCelebritiesViewModel
     ├─ @HiltViewModel
     ├─ repo: CelebrityRepository
     │
     └─ popularCelebrities: Flow<PagingData<Celebrity>>
        = repo.popularCelebritiesPagingDataSource()
          .cachedIn(viewModelScope)

Repository:
  └─ popularCelebritiesPagingDataSource()
     └─ Pager(
          pagingSourceFactory = {
            PopularCelebritiesPagingDataSource(apiService)
          },
          config = PagingConfig(pageSize = 20)
        ).flow

CelebrityPagingDataSource:
  └─ apiService.popularCelebrities(page)
     └─ @GET("person/popular")
        Response: BaseModel<Celebrity>
        
        Celebrity:
        ├─ id: Int
        ├─ name: String
        ├─ profilePath: String?
        ├─ knownForDepartment: String
        ├─ knownFor: List<KnownFor>  // Các phim/series nổi tiếng
        └─ popularity: Double

Click vào Celebrity:
  └─ ArtistDetailViewModel
     └─ artistDetail(personId: Int): Flow<DataState<ArtistDetail>>
        ├─ apiService.artistDetail(personId)
        └─ @GET("person/{personId}")
           Response: ArtistDetail
           
           ArtistDetail:
           ├─ id: Int
           ├─ name: String
           ├─ biography: String
           ├─ birthday: String
           ├─ deathday: String?
           ├─ profilePath: String?
           ├─ homepage: String?
           ├─ imdbId: String?
           └─ placeOfBirth: String?
```

---

## 🔧 DEPENDENCY INJECTION FLOW

### **Hilt Configuration**
```
File: HiltApplication.kt
@HiltAndroidApp
class HiltApplication : Application()
  └─ Hilt initialize khi app start

File: MainActivity.kt
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    /* Hilt tự inject dependencies */
  }
}

@Composable Screen:
  └─ val viewModel: MovieListViewModel = hiltViewModel()
     └─ Hilt tự tạo instance + inject dependencies
```

### **Network Module (Hilt DI)**
```
File: di/NetworkModule.kt

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

  @Singleton
  @Provides
  fun provideBaseURL(): String = ApiURL.BASE_URL
  
  @Singleton
  @Provides
  fun provideLoggingInterceptor(): HttpLoggingInterceptor {
    return HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
  }
  
  @Singleton
  @Provides
  fun provideApiKeyInterceptor(): ApiKeyInterceptor {
    return ApiKeyInterceptor()
  }
  
  @Singleton
  @Provides
  fun provideOkHttpClient(
    loggingInterceptor: HttpLoggingInterceptor,
    apiKeyInterceptor: ApiKeyInterceptor,
  ): OkHttpClient {
    return OkHttpClient().newBuilder()
      .callTimeout(40, TimeUnit.SECONDS)
      .connectTimeout(40, TimeUnit.SECONDS)
      .readTimeout(40, TimeUnit.SECONDS)
      .writeTimeout(40, TimeUnit.SECONDS)
      .addInterceptor(loggingInterceptor)
      .addInterceptor(apiKeyInterceptor)
      .build()
  }
  
  @Singleton
  @Provides
  fun provideConverterFactory(): Converter.Factory {
    return GsonConverterFactory.create()
  }
  
  @Singleton
  @Provides
  fun provideRetrofitClient(
    baseUrl: String,
    okHttpClient: OkHttpClient,
    converterFactory: Converter.Factory,
  ): Retrofit {
    return Retrofit.Builder()
      .baseUrl(baseUrl)
      .client(okHttpClient)
      .addConverterFactory(converterFactory)
      .build()
  }
  
  @Singleton
  @Provides
  fun provideRestApiService(retrofit: Retrofit): ApiService {
    return retrofit.create(ApiService::class.java)
  }
}

Dependency Chain:
BaseURL ← Retrofit ← OkHttpClient ← (LoggingInterceptor + ApiKeyInterceptor)
                ↓
            Converter Factory (GSON)
                ↓
            ApiService (Retrofit interface)
```

### **Repository Module (Hilt DI)**
```
File: di/RepositoryModule.kt

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

  @Provides
  @Singleton
  fun provideMovieRepository(
    apiService: ApiService  // Inject từ NetworkModule
  ): MovieRepository {
    return MovieRepositoryImpl(apiService)
  }
  
  @Provides
  @Singleton
  fun provideTvSeriesRepository(apiService: ApiService): TvSeriesRepository {
    return TvSeriesRepositoryImpl(apiService)
  }
  
  /* Tương tự cho CelebrityRepository, ArtistRepository, ... */
}
```

### **Database Module (Hilt DI)**
```
File: di/DataBaseModule.kt

@Module
@InstallIn(SingletonComponent::class)
object DataBaseModule {

  @Provides
  @Singleton
  fun provideMovieDatabase(context: Context): MovieDatabase {
    return Room.databaseBuilder(
      context,
      MovieDatabase::class.java,
      "MovieWorldDataBase"
    ).build()
  }
  
  @Provides
  fun provideFavoriteMovieDao(db: MovieDatabase): FavoriteMovieDao {
    return db.getFavoriteMovieDetailDao()
  }
  
  @Provides
  fun provideFavoriteTvSeriesDao(db: MovieDatabase): FavoriteTvSeriesDao {
    return db.getFavoriteTvSeriesDao()
  }
  
  @Provides
  @Singleton
  fun provideLocalMovieRepository(
    movieDao: FavoriteMovieDao
  ): LocalMovieRepository {
    return LocalMovieRepositoryImpl(movieDao)
  }
}
```

---

## 📊 KIẾN TRÚC TỔNG QUÁT - LAYER BY LAYER

### **1️⃣ PRESENTATION LAYER (UI - Jetpack Compose)**

**Location:** `ui/screens/`

**Files:**
- `MainActivity.kt` - Activity chính, @AndroidEntryPoint
- `NavGraph.kt` - Navigation graph định nghĩa toàn bộ routes
- `mainscreen/MainScreen.kt` - Tab navigation (Movies, TV, Celebrities, Favorites)
- `mainscreen/tav_view/TabView.kt` - Tab bar UI
- Screens:
  - Movies: `nowplaying/NowPlayingMovie.kt`, `popular/PopularMovie.kt`, `toprated/TopRatedMovie.kt`, `upcoming/UpComingMovie.kt`
  - Movie Detail: `movies/movie_detail/MovieDetail.kt`
  - TV Series: Airing Today, On The Air, Popular, Top Rated
  - TV Series Detail: `tv_series/tv_series_detail/TvSeriesDetail.kt`
  - Celebrities: Popular, Trending
  - Artist Detail: `artist_detail/ArtistDetail.kt`
  - Favorites: `favorite/movie/FavoriteMovie.kt`, `favorite/tvseries/FavoriteTvSeries.kt`

**Composable Pattern:**
```kotlin
@Composable
fun NowPlayingMovie(
  viewModel: NowPlayingMovieViewModel = hiltViewModel(),
  onMovieDetailClick: (Int) -> Unit
) {
  // Collect state
  val uiState by viewModel.uiState.collectAsState()
  val nowPlayingMovies = viewModel.nowPlayingMovies.collectAsLazyPagingItems()
  
  // Render UI
  if (uiState.isLoading) {
    LoadingScreen()
  } else if (uiState.errorMessage != null) {
    ErrorScreen(uiState.errorMessage)
  } else {
    LazyColumn {
      items(nowPlayingMovies.itemCount) { index ->
        MovieItemCard(
          movie = nowPlayingMovies[index],
          onClick = { onMovieDetailClick(nowPlayingMovies[index]?.id ?: 0) }
        )
      }
    }
  }
}
```

---

### **2️⃣ VIEWMODEL LAYER**

**Location:** `ui/screens/[feature]/`

**Pattern:** @HiltViewModel (Hilt tự động inject)

**Responsibility:**
- Quản lý UI state
- Gọi Repository để lấy dữ liệu
- Xử lý user events
- Cache data trong scope của viewmodel

**Example - NowPlayingMovieViewModel:**
```kotlin
@HiltViewModel
class NowPlayingMovieViewModel @Inject constructor(
  private val repo: MovieRepository,
) : ViewModel() {
  
  // État
  private val _uiState = MutableStateFlow(UiState())
  val uiState: StateFlow<UiState> = _uiState.asStateFlow()
  
  val selectedGenre = mutableStateOf(Genre(null, "All"))
  val filterData = MutableStateFlow<GenreId?>(null)
  
  // Collections dữ liệu (lazy-loaded pagination)
  @OptIn(ExperimentalCoroutinesApi::class)
  val nowPlayingMovies = filterData.flatMapLatest {
    repo.nowPlayingMoviePagingDataSource(it?.genreId)
  }.cachedIn(viewModelScope)
  
  // Methods
  fun onGenreSelected(genre: Genre?) {
    filterData.value = GenreId(genre?.id.toString())
  }
  
  fun updateLoadState(loadState: CombinedLoadStates) {
    val isLoading = loadState.refresh is LoadState.Loading
    _uiState.value = UiState(isLoading = isLoading)
  }
}
```

**Lifecycle:**
```
viewModel created → init block (nếu có) → recomposition occurs
   ↓
Collect state → Render UI
   ↓
User interaction → Call viewModel method
   ↓
viewModel destroyed (when Activity/Nav back)
```

---

### **3️⃣ REPOSITORY LAYER**

**Location:** `data/repository/`

**Pattern:**
- Interface + Implementation
- Constructor injection @Inject
- Abstraction for data sources

**Remote Repositories (API Data):**

```kotlin
// Interface
interface MovieRepository {
  suspend fun movieDetail(movieId: Int): Flow<DataState<MovieDetail>>
  suspend fun recommendedMovie(movieId: Int): Flow<DataState<List<MovieItem>>>
  fun nowPlayingMoviePagingDataSource(genreId: String?): Flow<PagingData<MovieItem>>
  // ... more methods
}

// Implementation
class MovieRepositoryImpl @Inject constructor(
  private val apiService: ApiService,
) : MovieRepository {
  
  override suspend fun movieDetail(movieId: Int): Flow<DataState<MovieDetail>> =
    safeApiCall { apiService.movieDetail(movieId) }
  
  override fun nowPlayingMoviePagingDataSource(genreId: String?): Flow<PagingData<MovieItem>> =
    Pager(
      pagingSourceFactory = { NowPlayingMoviePagingDataSource(apiService, genreId) },
      config = PagingConfig(pageSize = 20)
    ).flow
  
  // ... implementations
}
```

**Local Repositories (Database):**

```kotlin
interface LocalMovieRepository {
  suspend fun favoriteMovies(): List<MovieDetail?>
  suspend fun removeMovieById(movieId: Int)
}

class LocalMovieRepositoryImpl @Inject constructor(
  private val movieDao: FavoriteMovieDao,
) : LocalMovieRepository {
  
  override suspend fun favoriteMovies(): List<MovieDetail?> {
    return movieDao.getAllMovieDetails()
  }
  
  override suspend fun removeMovieById(movieId: Int) {
    movieDao.deleteMovieDetailById(movieId)
  }
}
```

---

### **4️⃣ DATASOURCE LAYER**

#### **A. Remote DataSource - Paging Sources**

**Location:** `data/datasource/remote/paging_datasource/`

**Pattern:** `PagingSource<Int, T>` - lazy load implementation

```kotlin
class NowPlayingMoviePagingDataSource(
  private val apiService: ApiService,
  private val genreId: String?
) : PagingSource<Int, MovieItem>() {
  
  override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MovieItem> {
    return try {
      val page = params.key ?: 1
      val response = apiService.nowPlayingMovies(page, genreId)
      
      LoadResult.Page(
        data = response.results,
        prevKey = if (page > 1) page - 1 else null,
        nextKey = if (response.results.isNotEmpty()) page + 1 else null
      )
    } catch (e: Exception) {
      LoadResult.Error(e)
    }
  }
}
```

#### **B. Network Layer - Retrofit + OkHttp**

**Location:** `network/`, `data/datasource/remote/`

**Files:**
- `ApiService.kt` - Retrofit interface với 27+ endpoints
- `ApiKeyInterceptor.kt` - Inject API key vào mỗi request
- `ApiURL.kt` - Định nghĩa base URL và endpoints

**ApiService Example:**
```kotlin
interface ApiService {
  @GET("movie/now_playing")
  suspend fun nowPlayingMovies(
    @Query("page") page: Int,
    @Query("with_genres") genreId: String?
  ): BaseModel<MovieItem>
  
  @GET("movie/{movieId}")
  suspend fun movieDetail(
    @Path("movieId") movieId: Int
  ): MovieDetail
  
  @GET("search/movie")
  suspend fun searchMovie(
    @Query("query") query: String
  ): SearchBaseModel
  
  // ... 24+ more endpoints
}
```

**ApiKeyInterceptor:**
```kotlin
class ApiKeyInterceptor : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val originalRequest = chain.request()
    val url = originalRequest.url.newBuilder()
      .addQueryParameter("api_key", BuildConfig.API_KEY)
      .build()
    val newRequest = originalRequest.newBuilder().url(url).build()
    return chain.proceed(newRequest)
  }
}
```

#### **C. Local DataSource - Room Database**

**Location:** `data/datasource/local/`

**Database:**
```kotlin
@Database(
  version = 1,
  entities = [MovieDetail::class, TvSeriesDetail::class],
  exportSchema = false
)
@TypeConverters(MovieTypeConverter::class, TvSeriesTypeConverter::class)
abstract class MovieDatabase : RoomDatabase() {
  abstract fun getFavoriteMovieDetailDao(): FavoriteMovieDao
  abstract fun getFavoriteTvSeriesDao(): FavoriteTvSeriesDao
}
```

**DAO:**
```kotlin
@Dao
interface FavoriteMovieDao {
  
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMovie(movieDetail: MovieDetail)
  
  @Query("SELECT * FROM MovieDetail")
  suspend fun getAllMovieDetails(): List<MovieDetail>
  
  @Query("DELETE FROM MovieDetail WHERE id = :movieId")
  suspend fun deleteMovieDetailById(movieId: Int)
}
```

**TypeConverters (Convert Complex Objects to JSON):**
```kotlin
class MovieTypeConverter {
  @TypeConverter
  fun fromGenreList(genres: List<Genre>?): String? {
    return Gson().toJson(genres)
  }
  
  @TypeConverter
  fun toGenreList(json: String?): List<Genre>? {
    return Gson().fromJson(json, object : TypeToken<List<Genre>>() {}.type)
  }
}
```

---

### **5️⃣ DATA MODEL LAYER**

**Location:** `data/model/`

**Categories:**

**Basic Models (for lists):**
```kotlin
@JsonClass(generateAdapter = true)
data class MovieItem(
  @SerializedName("id")
  val id: Int,
  @SerializedName("title")
  val title: String,
  @SerializedName("poster_path")
  val posterPath: String?,
  @SerializedName("backdrop_path")
  val backdropPath: String?,
  @SerializedName("overview")
  val overview: String?,
  @SerializedName("release_date")
  val releaseDate: String?,
  @SerializedName("vote_average")
  val voteAverage: Double?,
)
```

**Detail Models (with entity annotation for Room):**
```kotlin
@Entity
@TypeConverters(MovieTypeConverter::class)
@JsonClass(generateAdapter = true)
data class MovieDetail(
  @PrimaryKey
  @SerializedName("id")
  val id: Int,
  @SerializedName("title")
  val title: String,
  @SerializedName("overview")
  val overview: String,
  @SerializedName("runtime")
  val runtime: Int,
  @SerializedName("budget")
  val budget: Long,
  @SerializedName("revenue")
  val revenue: Long,
  @SerializedName("genres")
  val genres: List<Genre>,
  // ... more fields
)
```

**Generic Wrapper Models:**
```kotlin
data class BaseModel<T>(
  val page: Int,
  val results: List<T>,
  @SerializedName("total_pages")
  val totalPages: Int,
  @SerializedName("total_results")
  val totalResults: Int,
)

data class SearchBaseModel(
  val page: Int,
  val results: List<MovieItem>,
  @SerializedName("total_pages")
  val totalPages: Int,
  @SerializedName("total_results")
  val totalResults: Int,
)
```

---

### **6️⃣ DEPENDENCY INJECTION (Hilt) LAYER**

**Location:** `di/`

**Files:**
- `NetworkModule.kt` - Retrofit, OkHttp, Interceptors
- `RepositoryModule.kt` - Repository bindings
- `DataBaseModule.kt` - Room Database
- `HiltApplication.kt` - @HiltAndroidApp

**Flow:**
```
1. HiltApplication starts
   └─ @HiltAndroidApp initializes Hilt

2. MainActivity launches
   └─ @AndroidEntryPoint enables dependency injection

3. Composable calls hiltViewModel<MovieListViewModel>()
   └─ Hilt queries DI graph:
      
      i.   @Provides provideRestApiService(retrofit)
           ↓ returns ApiService
           
      ii.  @Provides provideMovieRepository(apiService)
           ↓ returns MovieRepository instance
           
      iii. @HiltViewModel MovieListViewModel(repo)
           ↓ Constructor injection happens
           
      iv.  ViewModel instance returned to Composable

4. ViewModel can now call repo methods
```

---

## 🔄 COMPLETE DATA FLOW DIAGRAM

```
┌────────────────────────────────────────────────────────────────────┐
│                         USER INTERACTION                            │
│              (Open app, Click movie, Search, etc)                   │
└─────────────────────────────────┬────────────────────────────────┘
                                  │
                                  ▼
┌────────────────────────────────────────────────────────────────────┐
│                      UI LAYER (Jetpack Compose)                     │
│                                                                      │
│  - Renders Composable functions                                     │
│  - Collects StateFlow/Flow from ViewModel                           │
│  - Handles user interactions                                        │
│  - Shows loading/error states                                       │
└─────────────────────────────────┬────────────────────────────────┘
                                  │
                                  ▼
┌────────────────────────────────────────────────────────────────────┐
│               VIEWMODEL LAYER (@HiltViewModel)                      │
│                                                                      │
│  - Saves UI state (StateFlow, MutableStateFlow)                     │
│  - Calls Repository methods                                         │
│  - Caches Paging data with cachedIn(viewModelScope)                │
│  - Handles pagination logic                                         │
│  - Lifecycle: survives configuration changes                        │
└─────────────────────────────────┬────────────────────────────────┘
                                  │
                    ┌─────────────┴──────────────┐
                    │                            │
                    ▼                            ▼
    ┌───────────────────────────────┐  ┌────────────────────────┐
    │  REPOSITORY - Remote (API)    │  │  REPOSITORY - Local    │
    │                               │  │  (Database)            │
    │  - MovieRepository            │  │                        │
    │  - TvSeriesRepository         │  │  - LocalMovieRepository│
    │  - CelebrityRepository        │  │  - LocalTvSeriesRepo  │
    │  - ArtistRepository           │  │                        │
    │                               │  │  Calls:                │
    │  Calls:                       │  │  - movieDao.insert()   │
    │  - apiService methods         │  │  - movieDao.getAll()   │
    │  - Creates Pager + PagingDS   │  │  - movieDao.delete()   │
    └─────────────────┬─────────────┘  └────────────┬───────────┘
                      │                              │
                      ▼                              ▼
    ┌───────────────────────────────┐  ┌────────────────────────┐
    │  PAGING DATASOURCE            │  │  ROOM DATABASE         │
    │  (lazy loading implementation) │  │                        │
    │                               │  │  - MovieDetail entity  │
    │  - NowPlayingMovieDataSource  │  │  - TvSeriesDetail       │
    │  - PopularMovieDataSource     │  │  - FavoriteMovieDao    │
    │  - SearchMovieDataSource      │  │  - FavoriteTvSeriesDao │
    │  - etc (22+ types)            │  │                        │
    │                               │  │  TypeConverters:       │
    │  Returns: LoadResult.Page()   │  │  - JSON ↔ Objects      │
    └─────────────────┬─────────────┘  └────────────┬───────────┘
                      │                              │
                      ▼                              ▼
    ┌───────────────────────────────┐  ┌────────────────────────┐
    │  NETWORK LAYER (Retrofit API) │  │  DEVICE LOCAL STORAGE  │
    │                               │  │                        │
    │  - ApiService (interface)     │  │  - SQLite Database     │
    │  - Retrofit client            │  │  - Room DDL/DML ops    │
    │  - OkHttpClient               │  │                        │
    │  - HttpLoggingInterceptor     │  │  Speed: ~milliseconds  │
    │  - ApiKeyInterceptor          │  │  Consistency: ACID     │
    │                               │  │                        │
    │  HTTP Calls:                  │  │                        │
    │  - GET /movie/now_playing     │  │                        │
    │  - GET /movie/{id}            │  │                        │
    │  - GET /search/movie          │  │                        │
    │  - etc (27+ endpoints)        │  │                        │
    └─────────────────┬─────────────┘  └────────────────────────┘
                      │
                      ▼
    ┌───────────────────────────────┐
    │  EXTERNAL DATA SOURCES        │
    │                               │
    │  - TMDB API Server            │
    │    (api.themoviedb.org)       │
    │                               │
    │  Returns: JSON Response       │
    │                               │
    │  Deserialized via GSON into   │
    │  Kotlin Data Classes          │
    └───────────────────────────────┘
```

---

## 📋 SUMMARY TABLE - ĐỒ VẬT -> LỚP -> CHỨC NĂNG

| Layer | Location | Class | Chức năng |
|-------|----------|-------|----------|
| **UI** | `ui/screens/movies/` | `NowPlayingMovie.kt` | Hiển thị danh sách phim, handle scroll |
| **UI** | `ui/screens/activity/` | `MainActivity.kt` | Activity chính, navigation setup |
| **ViewModel** | `ui/screens/movies/nowplaying/` | `NowPlayingMovieViewModel` | Quản lý state, lazy load paging data |
| **Repository Remote** | `data/repository/remote/movie/` | `MovieRepositoryImpl` | Fetch dữ liệu từ API, tạo Pager |
| **Paging Source** | `data/datasource/remote/paging_datasource/` | `NowPlayingMoviePagingDataSource` | Load 1 trang (20 items) từ API |
| **Network** | `data/datasource/remote/` | `ApiService` | Retrofit interface, 27 endpoints |
| **Network** | `network/` | `ApiKeyInterceptor` | Inject API key vào HTTP requests |
| **Local DB** | `data/datasource/local/` | `MovieWorldDatabase` | Room database, 2 entities |
| **DAO** | `data/datasource/local/dao/` | `FavoriteMovieDao` | Insert/Query/Delete favorite movies |
| **Model** | `data/model/` | `MovieItem` | Basic movie data (title, poster, etc) |
| **Model** | `data/model/moviedetail/` | `MovieDetail` | Full movie info, Room @Entity |
| **DI** | `di/` | `NetworkModule` | Provide Retrofit, OkHttp, ApiService |
| **DI** | `di/` | `RepositoryModule` | Provide Repository instances |
| **DI** | `di/` | `DataBaseModule` | Provide Room Database |

---

## 🎯 KEY TECHNOLOGIES USED

| Technology | Purpose | Location |
|-----------|---------|----------|
| **Jetpack Compose** | Modern UI framework (declarative) | `ui/` |
| **MVVM** | Architecture pattern | ViewModels, UI, Repositories |
| **Hilt** | Dependency Injection | `di/`, `@HiltViewModel`, `@Inject` |
| **Retrofit** | REST API client | `data/datasource/remote/ApiService` |
| **OkHttp** | HTTP client (Retrofit backend) | `di/NetworkModule` |
| **Room** | Local SQL database | `data/datasource/local/` |
| **Paging 3** | Lazy pagination | `PagingSource`, `Pager` |
| **Coroutines** | Async/reactive | `suspend`, `Flow`, `StateFlow` |
| **GSON** | JSON serialization | Data models, API responses |
| **Timber** | Logging | HTTP logging, debug |

---

## 🌐 API ENDPOINTS - 27+ Endpoints

**Base URL:** `https://api.themoviedb.org/3/`

### Movies (8 endpoints)
- `GET /movie/now_playing` - Phim đang phát hành
- `GET /movie/popular` - Phim phổ biến
- `GET /movie/top_rated` - Phim xếp hạng cao
- `GET /movie/upcoming` - Phim sắp tới
- `GET /movie/{id}` - Chi tiết phim
- `GET /movie/{id}/recommendations` - Phim tương tự
- `GET /movie/{id}/credits` - Diễn viên/crew
- `GET /search/movie` - Tìm phim

### TV Series (6 endpoints)
- `GET /tv/airing_today` - Series phát sóng hôm nay
- `GET /tv/on_the_air` - Series đang phát sóng
- `GET /tv/popular` - Series phổ biến
- `GET /tv/top_rated` - Series xếp hạng cao
- `GET /tv/{id}` - Chi tiết series
- `GET /tv/{id}/credits` - Diễn viên/crew

### People/Celebrities (5+ endpoints)
- `GET /person/popular` - People nổi tiếng
- `GET /person/trending` - People trending
- `GET /person/{id}` - Chi tiết người
- `GET /person/{id}/movie_credits` - Phim của người
- ...

### Genres (1+ endpoint)
- `GET /genre/movie/list` - Danh sách thể loại phim
- Pagination: Sử dụng query param `?page=1&with_genres=28`

---

## 💾 DATABASE SCHEMA (Room SQLite)

```
DATABASE: MovieWorldDatabase (version 1)

TABLE: MovieDetail (Entity)
├─ id (INT, PRIMARY KEY)
├─ title (TEXT)
├─ overview (TEXT)
├─ posterPath (TEXT)
├─ backdropPath (TEXT)
├─ releaseDate (TEXT)
├─ runtime (INT)
├─ budget (LONG)
├─ revenue (LONG)
├─ voteAverage (DOUBLE)
├─ voteCount (INT)
├─ homepage (TEXT)
├─ status (TEXT)
├─ genres (TEXT, JSON via TypeConverter)
├─ productionCompanies (TEXT, JSON)
├─ productionCountries (TEXT, JSON)
└─ spokenLanguages (TEXT, JSON)

TABLE: TvSeriesDetail (Entity)
├─ id (INT, PRIMARY KEY)
├─ name (TEXT)
├─ overview (TEXT)
├─ posterPath (TEXT)
├─ backdropPath (TEXT)
├─ firstAirDate (TEXT)
├─ genres (TEXT, JSON)
├─ seasons (TEXT, JSON)
├─ createdBy (TEXT, JSON)
├─ networks (TEXT, JSON)
└─ ... (20+ fields)

TypeConverters:
├─ MovieTypeConverter (handles nested Genre, ProductionCompany, etc)
└─ TvSeriesTypeConverter (handles nested Season, CreatedBy, etc)
```

---

## 🔑 KEY POINTS TO UNDERSTAND

1. **Separation of Concerns:**
   - UI: chỉ render data + handle user input
   - ViewModel: manage state + call repository
   - Repository: abstract data sources
   - Network/Database: actual data fetching/storing

2. **Dependency Injection (Hilt):**
   - Tất cả @Inject đều tự động được inject bởi Hilt
   - Modules (NetworkModule, RepositoryModule) định nghĩa cách tạo instances
   - Giúp testable, flexible, reduce boilerplate

3. **Data Flow - Single Direction:**
   ```
   API/Database → Repository → ViewModel (StateFlow) → UI (Composable)
   
   User Action → ViewModel Method → Repository Call → State Update → Recompose
   ```

4. **Pagination:**
   - Hilt-MVVM app sử dụng AndroidX Paging 3
   - `PagingSource` tự động load trang tiếp theo khi user scroll
   - `cachedIn(viewModelScope)` giữ cache trong ViewModel lifetime

5. **Local Storage (Room Database):**
   - Favorites được lưu locally
   - TypeConverters convert JSON ↔ Kotlin objects
   - DAOs provide strongly-typed queries

6. **Hot/Cold Flows:**
   - `Flow<T>` = cold flow (execute khi collect)
   - `StateFlow<T>` = hot flow (always emit latest value)
   - UI collects StateFlow để observe state changes

---

## 🎬 COMPLETE EXAMPLE: USER OPENS APP

```
1. User launches app
   ↓
2. Android creates MainActivity (@AndroidEntryPoint)
   ↓ Hilt injects dependencies
3. MainActivity renders MainScreen Composable
   ↓
4. MainScreen shows TabView (Movies, TV, People, Favorites)
   ↓
5. Default tab = "Now Playing Movies"
   ↓
6. NowPlayingMovie Composable loads
   ↓ 
7. hiltViewModel<NowPlayingMovieViewModel>()
   └─ Hilt DI Graph:
      - Queries NetworkModule
      - Gets ApiService instance
      - Queries RepositoryModule
      - Gets MovieRepository instance
      - Queries HiltViewModel constructor
      - Injects repo → Creates NowPlayingMovieViewModel
   ↓
8. ViewModel init:
   ```
   @OptIn(ExperimentalCoroutinesApi::class)
   val nowPlayingMovies = filterData.flatMapLatest {
     repo.nowPlayingMoviePagingDataSource(it?.genreId)
   }.cachedIn(viewModelScope)
   ```
   └─ Registers flow collector
   ↓
9. Repository creates Flow<PagingData<MovieItem>>
   ```
   Pager(
     pagingSourceFactory = { 
       NowPlayingMoviePagingDataSource(apiService, genreId) 
     },
     config = PagingConfig(pageSize = 20)
   ).flow
   ```
   ↓
10. Paging system loads first page:
    - PagingDataSource.load(params)
    - params.key = 1 (first page)
    └─ API call: GET /movie/now_playing?page=1&api_key=...
       ↓ (Interceptor adds api_key)
       ↓ Http request → TMDB Server
       ↓ JSON Response with 20 movies
       ↓ GSON deserializes → List<MovieItem>
       ↓ Returns LoadResult.Page(data, prevKey, nextKey)
    ↓
11. Paging merges pages into PagingData<MovieItem>
    ↓ 
12. ViewModel's nowPlayingMovies flow emits PagingData
    ↓
13. Composable collects:
    ```
    val lazyPagingItems = nowPlayingMovies.collectAsLazyPagingItems()
    ```
    ↓
14. LazyColumn renders items:
    ```
    items(lazyPagingItems.itemCount) { index ->
      MovieCard(lazyPagingItems[index])
    }
    ```
    ↓
15. Screen shows 20 movies with beautiful thumbnails
    ↓
16. User scrolls down
    ↓
17. LazyPagingItems automatically detects end-of-list
    ↓
18. Triggers load(params with page=2)
    ↓
19. Fetches next 20 movies...
    ↓
    (Cycle repeats)
    ↓
20. User clicks on movie
    ↓
21. Navigation to MovieDetailScreen(movieId)
    ↓
22. MovieDetailViewModel loads full details:
    - repo.movieDetail(movieId)
    - repo.recommendedMovie(movieId)
    - repo.movieCredit(movieId)
    └─ Multiple API calls in parallel
    ↓
23. Display full movie info + cast + recommendations
    ↓
24. User clicks "Add to Favorites"
    ↓
25. ViewModel calls:
    localMovieRepository.saveMovie(movieDetail)
    ↓
26. DAOInserts into Room database
    └─ SQL: INSERT INTO MovieDetail (id, title, ...) VALUES (...)
    ↓
27. Success, show toast "Added to favorites"
    ↓
(User can view favorites anytime without API call - offline)
```

---

## 🔗 FILE ORGANIZATION SUMMARY

```
com/piashcse/hilt_mvvm_compose_movie/
│
├─ ui/
│  ├─ screens/
│  │  ├─ activity/MainActivity.kt (@AndroidEntryPoint)
│  │  ├─ movies/
│  │  │  ├─ nowplaying/ (NowPlayingMovie.kt, ViewModel)
│  │  │  ├─ popular/ (PopularMovie.kt, ViewModel)
│  │  │  ├─ toprated/ (TopRatedMovie.kt, ViewModel)
│  │  │  ├─ upcoming/ (UpComingMovie.kt, ViewModel)
│  │  │  └─ movie_detail/ (MovieDetail.kt, ViewModel)
│  │  ├─ tv_series/
│  │  │  ├─ airing_today/, on_the_air/, popular/, top_rated/
│  │  │  └─ tv_series_detail/
│  │  ├─ celebrities/
│  │  │  ├─ popular/, trending/
│  │  │  └─ ArtistDetail.kt
│  │  ├─ favorite/
│  │  │  ├─ movie/FavoriteMovie.kt
│  │  │  └─ tvseries/FavoriteTvSeries.kt
│  │  ├─ mainscreen/
│  │  │  ├─ MainScreen.kt
│  │  │  └─ tav_view/TabView.kt
│  │  └─ state/UiState.kt (data class)
│  └─ components/ (Reusable composables)
│
├─ data/
│  ├─ model/
│  │  ├─ MovieItem.kt, TvSeriesItem.kt
│  │  ├─ moviedetail/MovieDetail.kt, Genre.kt
│  │  ├─ tv_series_detail/TvSeriesDetail.kt
│  │  ├─ celebrities/Celebrity.kt
│  │  ├─ artist/Artist.kt, ArtistDetail.kt
│  │  ├─ BaseModel.kt, SearchBaseModel.kt, Genres.kt
│  │  └─ GenreId.kt
│  │
│  ├─ repository/
│  │  ├─ remote/
│  │  │  ├─ movie/ (MovieRepository.kt, MovieRepositoryImpl.kt)
│  │  │  ├─ tvseries/ (TvSeriesRepository.kt, TvSeriesRepositoryImpl.kt)
│  │  │  ├─ celebrity/ (CelebrityRepository.kt, CelebrityRepositoryImpl.kt)
│  │  │  └─ artist/ (ArtistRepository.kt, ArtistRepositoryImpl.kt)
│  │  └─ local/
│  │     ├─ movie/ (LocalMovieRepository.kt, LocalMovieRepositoryImpl.kt)
│  │     └─ tvseries/ (LocalTvSeriesRepository.kt, LocalTvSeriesRepositoryImpl.kt)
│  │
│  └─ datasource/
│     ├─ remote/
│     │  ├─ ApiService.kt (Retrofit interface)
│     │  ├─ ApiURL.kt (Base URL + constants)
│     │  └─ paging_datasource/
│     │     ├─ movie/ (NowPlayingMoviePagingDataSource, PopularMoviePagingDataSource, etc)
│     │     ├─ tv_series/ (AiringTodayTvSeriesPagingDataSource, etc)
│     │     ├─ celebrities/ (PopularCelebritiesPagingDataSource, etc)
│     │     └─ GenrePagingDataSource.kt
│     └─ local/
│        ├─ MovieWorldDataBase.kt (Room Database)
│        ├─ dao/ (FavoriteMovieDao.kt, FavoriteTvSeriesDao.kt)
│        └─ typeconverter/ (MovieTypeConverter.kt, TvSeriesTypeConverter.kt)
│
├─ di/
│  ├─ NetworkModule.kt (Retrofit, OkHttp, Interceptors)
│  ├─ RepositoryModule.kt (Repository bindings)
│  └─ DataBaseModule.kt (Room Database, DAOs)
│
├─ network/
│  └─ ApiKeyInterceptor.kt (HTTP interceptor)
│
├─ navigation/
│  ├─ NavGraph.kt (Navigation routes)
│  └─ NavItems.kt (Route definitions)
│
├─ utils/
│  ├─ AppConstant.kt
│  ├─ network/
│  │  └─ DataState.kt (Sealed class for API response)
│  └─ safeApiCall.kt (Safe API wrapper)
│
└─ HiltApplication.kt (@HiltAndroidApp)
```

---

**Tài liệu này cung cấp bản đồ đầy đủ về kiến trúc dữ liệu của ứng dụng, từ API đến UI. Sử dụng nó như tài liệu tham khảo để hiểu luồng dữ liệu qua các lớp khác nhau.**

