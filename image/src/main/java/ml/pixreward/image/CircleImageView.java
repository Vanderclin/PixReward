package ml.pixreward.image;

import android.annotation.*;
import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.*;
import android.os.*;
import android.support.annotation.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import java.util.concurrent.*;

import android.annotation.NonNull;

public class CircleImageView extends ImageView
	{
		private static final int LOADING_THREADS = 4;
		private static ExecutorService threadPool = Executors.newFixedThreadPool(LOADING_THREADS);


		private static final ScaleType SCALE_TYPE = ScaleType.CENTER;

		private static final Bitmap.Config BITMAP_CONFIG = Bitmap.Config.ARGB_8888;
		private static final int COLORDRAWABLE_DIMENSION = 2;

		private static final int DEFAULT_BORDER_WIDTH = 0;
		private static final int DEFAULT_BORDER_COLOR = Color.BLACK;
		private static final int DEFAULT_CIRCLE_BACKGROUND_COLOR = Color.TRANSPARENT;
		private static final int DEFAULT_IMAGE_ALPHA = 255;
		private static final boolean DEFAULT_BORDER_OVERLAY = false;

		private final RectF mDrawableRect = new RectF();
		private final RectF mBorderRect = new RectF();

		private final Matrix mShaderMatrix = new Matrix();
		private final Paint mBitmapPaint = new Paint();
		private final Paint mBorderPaint = new Paint();
		private final Paint mCircleBackgroundPaint = new Paint();

		private int mBorderColor = DEFAULT_BORDER_COLOR;
		private int mBorderWidth = DEFAULT_BORDER_WIDTH;
		private int mCircleBackgroundColor = DEFAULT_CIRCLE_BACKGROUND_COLOR;
		private int mImageAlpha = DEFAULT_IMAGE_ALPHA;

		private Bitmap mBitmap;
		private Canvas mBitmapCanvas;

		private float mDrawableRadius;
		private float mBorderRadius;

		private ColorFilter mColorFilter;

		private boolean mInitialized;
		private boolean mRebuildShader;
		private boolean mDrawableDirty;

		private boolean mBorderOverlay;
		private boolean mDisableCircularTransformation;

		private SmartImageTask currentTask;


		public CircleImageView ( Context context )
			{
				super(context);
			}

		public CircleImageView ( Context context, AttributeSet attrs )
			{
				super(context, attrs, 0);
			}

		public CircleImageView ( Context context, AttributeSet attrs, int defStyle )
			{
				super(context, attrs, defStyle);

				TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CircleImageView, defStyle, 0);

				mBorderWidth = a.getDimensionPixelSize(R.styleable.CircleImageView_civ_border_width, DEFAULT_BORDER_WIDTH);
				mBorderColor = a.getColor(R.styleable.CircleImageView_civ_border_color, DEFAULT_BORDER_COLOR);
				mBorderOverlay = a.getBoolean(R.styleable.CircleImageView_civ_border_overlay, DEFAULT_BORDER_OVERLAY);
				mCircleBackgroundColor = a.getColor(R.styleable.CircleImageView_civ_circle_background_color, DEFAULT_CIRCLE_BACKGROUND_COLOR);

				a.recycle();

				init();


			}

		private void init ( )
			{
				mInitialized = true;

				super.setScaleType(SCALE_TYPE);

				mBitmapPaint.setAntiAlias(true);
				mBitmapPaint.setDither(true);
				mBitmapPaint.setFilterBitmap(true);
				mBitmapPaint.setAlpha(mImageAlpha);
				mBitmapPaint.setColorFilter(mColorFilter);

				mBorderPaint.setStyle(Paint.Style.STROKE);
				mBorderPaint.setAntiAlias(true);
				mBorderPaint.setColor(mBorderColor);
				mBorderPaint.setStrokeWidth(mBorderWidth);

				mCircleBackgroundPaint.setStyle(Paint.Style.FILL);
				mCircleBackgroundPaint.setAntiAlias(true);
				mCircleBackgroundPaint.setColor(mCircleBackgroundColor);

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
					{
						setOutlineProvider(new OutlineProvider());
					}
			}


		// Helpers to set image by URL
		public void setImageUrl ( String url )
			{
				setImage(new WebImage(url));
			}

		public void setImageUrl ( String url, SmartImageTask.OnCompleteListener completeListener )
			{
				setImage(new WebImage(url), completeListener);
			}

		public void setImageUrl ( String url, final Integer fallbackResource )
			{
				setImage(new WebImage(url), fallbackResource);
			}

		public void setImageUrl ( String url, final Integer fallbackResource, SmartImageTask.OnCompleteListener completeListener )
			{
				setImage(new WebImage(url), fallbackResource, completeListener);
			}

		public void setImageUrl ( String url, final Integer fallbackResource, final Integer loadingResource )
			{
				setImage(new WebImage(url), fallbackResource, loadingResource);
			}

		public void setImageUrl ( String url, final Integer fallbackResource, final Integer loadingResource, SmartImageTask.OnCompleteListener completeListener )
			{
				setImage(new WebImage(url), fallbackResource, loadingResource, completeListener);
			}


		// Helpers to set image by contact address book id
		public void setImageContact ( long contactId )
			{
				setImage(new ContactImage(contactId));
			}

		public void setImageContact ( long contactId, final Integer fallbackResource )
			{
				setImage(new ContactImage(contactId), fallbackResource);
			}

		public void setImageContact ( long contactId, final Integer fallbackResource, final Integer loadingResource )
			{
				setImage(new ContactImage(contactId), fallbackResource, fallbackResource);
			}


		// Set image using SmartImage object
		public void setImage ( final SmartImage image )
			{
				setImage(image, null, null, null);
			}

		public void setImage ( final SmartImage image, final SmartImageTask.OnCompleteListener completeListener )
			{
				setImage(image, null, null, completeListener);
			}

		public void setImage ( final SmartImage image, final Integer fallbackResource )
			{
				setImage(image, fallbackResource, fallbackResource, null);
			}

		public void setImage ( final SmartImage image, final Integer fallbackResource, SmartImageTask.OnCompleteListener completeListener )
			{
				setImage(image, fallbackResource, fallbackResource, completeListener);
			}

		public void setImage ( final SmartImage image, final Integer fallbackResource, final Integer loadingResource )
			{
				setImage(image, fallbackResource, loadingResource, null);
			}

		public void setImage ( final SmartImage image, final Integer fallbackResource, final Integer loadingResource, final SmartImageTask.OnCompleteListener completeListener )
			{
				// Set a loading resource
				if (loadingResource != null)
					{
						setImageResource(loadingResource);
					}

				// Cancel any existing tasks for this image view
				if (currentTask != null)
					{
						currentTask.cancel();
						currentTask = null;
					}

				// Set up the new task
				currentTask = new SmartImageTask(getContext(), image);
				currentTask.setOnCompleteHandler(new SmartImageTask.OnCompleteHandler() {
							@Override
							public void onComplete ( Bitmap bitmap )
								{
									if (bitmap != null)
										{
											setImageBitmap(bitmap);
										}
									else
										{
											// Set fallback resource
											if (fallbackResource != null)
												{
													setImageResource(fallbackResource);
												}
										}

									if (completeListener != null)
										{
											completeListener.onComplete(bitmap);
										}
								}
						});

				// Run the task in a threadpool
				threadPool.execute(currentTask);
			}

		public static void cancelAllTasks ( )
			{
				threadPool.shutdownNow();
				threadPool = Executors.newFixedThreadPool(LOADING_THREADS);
			}



		@Override
		public void setScaleType ( ScaleType scaleType )
			{
				if (scaleType != SCALE_TYPE)
					{
						throw new IllegalArgumentException(String.format("ScaleType %s not supported.", scaleType));
					}
			}

		@Override
		public void setAdjustViewBounds ( boolean adjustViewBounds )
			{
				if (adjustViewBounds)
					{
						throw new IllegalArgumentException("adjustViewBounds not supported.");
					}
			}

		@SuppressLint("CanvasSize")
		@Override
		protected void onDraw ( Canvas canvas )
			{
				if (mDisableCircularTransformation)
					{
						super.onDraw(canvas);
						return;
					}

				if (mCircleBackgroundColor != Color.TRANSPARENT)
					{
						canvas.drawCircle(mDrawableRect.centerX(), mDrawableRect.centerY(), mDrawableRadius, mCircleBackgroundPaint);
					}

				if (mBitmap != null)
					{
						if (mDrawableDirty && mBitmapCanvas != null)
							{
								mDrawableDirty = false;
								Drawable drawable = getDrawable();
								drawable.setBounds(0, 0, mBitmapCanvas.getWidth(), mBitmapCanvas.getHeight());
								drawable.draw(mBitmapCanvas);
							}

						if (mRebuildShader)
							{
								mRebuildShader = false;

								BitmapShader bitmapShader = new BitmapShader(mBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
								bitmapShader.setLocalMatrix(mShaderMatrix);

								mBitmapPaint.setShader(bitmapShader);
							}

						canvas.drawCircle(mDrawableRect.centerX(), mDrawableRect.centerY(), mDrawableRadius, mBitmapPaint);
					}

				if (mBorderWidth > 0)
					{
						canvas.drawCircle(mBorderRect.centerX(), mBorderRect.centerY(), mBorderRadius, mBorderPaint);
					}
			}

		@Override
		public void invalidateDrawable ( @NonNull Drawable dr )
			{
				mDrawableDirty = true;
				invalidate();
			}

		@Override
		protected void onSizeChanged ( int w, int h, int oldw, int oldh )
			{
				super.onSizeChanged(w, h, oldw, oldh);
				updateDimensions();
				invalidate();
			}

		@Override
		public void setPadding ( int left, int top, int right, int bottom )
			{
				super.setPadding(left, top, right, bottom);
				updateDimensions();
				invalidate();
			}

		@Override
		public void setPaddingRelative ( int start, int top, int end, int bottom )
			{
				super.setPaddingRelative(start, top, end, bottom);
				updateDimensions();
				invalidate();
			}

		public int getBorderColor ( )
			{
				return mBorderColor;
			}

		public void setBorderColor ( @ColorInt int borderColor )
			{
				if (borderColor == mBorderColor)
					{
						return;
					}

				mBorderColor = borderColor;
				mBorderPaint.setColor(borderColor);
				invalidate();
			}

		public int getCircleBackgroundColor ( )
			{
				return mCircleBackgroundColor;
			}

		public void setCircleBackgroundColor ( @ColorInt int circleBackgroundColor )
			{
				if (circleBackgroundColor == mCircleBackgroundColor)
					{
						return;
					}

				mCircleBackgroundColor = circleBackgroundColor;
				mCircleBackgroundPaint.setColor(circleBackgroundColor);
				invalidate();
			}

		/**
		 * @deprecated Use {@link #setCircleBackgroundColor(int)} instead
		 */
		@Deprecated
		public void setCircleBackgroundColorResource ( @ColorRes int circleBackgroundRes )
			{
				setCircleBackgroundColor(getContext().getResources().getColor(circleBackgroundRes));
			}

		public int getBorderWidth ( )
			{
				return mBorderWidth;
			}

		public void setBorderWidth ( int borderWidth )
			{
				if (borderWidth == mBorderWidth)
					{
						return;
					}

				mBorderWidth = borderWidth;
				mBorderPaint.setStrokeWidth(borderWidth);
				updateDimensions();
				invalidate();
			}

		public boolean isBorderOverlay ( )
			{
				return mBorderOverlay;
			}

		public void setBorderOverlay ( boolean borderOverlay )
			{
				if (borderOverlay == mBorderOverlay)
					{
						return;
					}

				mBorderOverlay = borderOverlay;
				updateDimensions();
				invalidate();
			}

		public boolean isDisableCircularTransformation ( )
			{
				return mDisableCircularTransformation;
			}

		public void setDisableCircularTransformation ( boolean disableCircularTransformation )
			{
				if (disableCircularTransformation == mDisableCircularTransformation)
					{
						return;
					}

				mDisableCircularTransformation = disableCircularTransformation;

				if (disableCircularTransformation)
					{
						mBitmap = null;
						mBitmapCanvas = null;
						mBitmapPaint.setShader(null);
					}
				else
					{
						initializeBitmap();
					}

				invalidate();
			}

		@Override
		public void setImageBitmap ( Bitmap bm )
			{
				super.setImageBitmap(bm);
				initializeBitmap();
				invalidate();
			}

		@Override
		public void setImageDrawable ( Drawable drawable )
			{
				super.setImageDrawable(drawable);
				initializeBitmap();
				invalidate();
			}

		@Override
		public void setImageResource ( @DrawableRes int resId )
			{
				super.setImageResource(resId);
				initializeBitmap();
				invalidate();
			}

		@Override
		public void setImageURI ( Uri uri )
			{
				super.setImageURI(uri);
				initializeBitmap();
				invalidate();
			}

		@Override
		public void setImageAlpha ( int alpha )
			{
				alpha &= 0xFF;

				if (alpha == mImageAlpha)
					{
						return;
					}

				mImageAlpha = alpha;

				// This might be called during ImageView construction before
				// member initialization has finished on API level >= 16.
				if (mInitialized)
					{
						mBitmapPaint.setAlpha(alpha);
						invalidate();
					}
			}

		@Override
		public int getImageAlpha ( )
			{
				return mImageAlpha;
			}

		@Override
		public void setColorFilter ( ColorFilter cf )
			{
				if (cf == mColorFilter)
					{
						return;
					}

				mColorFilter = cf;

				// This might be called during ImageView construction before
				// member initialization has finished on API level <= 19.
				if (mInitialized)
					{
						mBitmapPaint.setColorFilter(cf);
						invalidate();
					}
			}

		@Override
		public ColorFilter getColorFilter ( )
			{
				return mColorFilter;
			}

		private Bitmap getBitmapFromDrawable ( Drawable drawable )
			{
				if (drawable == null)
					{
						return null;
					}

				if (drawable instanceof BitmapDrawable)
					{
						return ((BitmapDrawable) drawable).getBitmap();
					}

				try
					{
						Bitmap bitmap;

						if (drawable instanceof ColorDrawable)
							{
								bitmap = Bitmap.createBitmap(COLORDRAWABLE_DIMENSION, COLORDRAWABLE_DIMENSION, BITMAP_CONFIG);
							}
						else
							{
								bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), BITMAP_CONFIG);
							}

						Canvas canvas = new Canvas(bitmap);
						drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
						drawable.draw(canvas);
						return bitmap;
					}
				catch (Exception e)
					{
						e.printStackTrace();
						return null;
					}
			}

		private void initializeBitmap ( )
			{
				mBitmap = getBitmapFromDrawable(getDrawable());

				if (mBitmap != null && mBitmap.isMutable())
					{
						mBitmapCanvas = new Canvas(mBitmap);
					}
				else
					{
						mBitmapCanvas = null;
					}

				if (!mInitialized)
					{
						return;
					}

				if (mBitmap != null)
					{
						updateShaderMatrix();
					}
				else
					{
						mBitmapPaint.setShader(null);
					}
			}

		private void updateDimensions ( )
			{
				mBorderRect.set(calculateBounds());
				mBorderRadius = Math.min((mBorderRect.height() - mBorderWidth) / 2.0f, (mBorderRect.width() - mBorderWidth) / 2.0f);

				mDrawableRect.set(mBorderRect);
				if (!mBorderOverlay && mBorderWidth > 0)
					{
						mDrawableRect.inset(mBorderWidth - 1.0f, mBorderWidth - 1.0f);
					}
				mDrawableRadius = Math.min(mDrawableRect.height() / 2.0f, mDrawableRect.width() / 2.0f);

				updateShaderMatrix();
			}

		private RectF calculateBounds ( )
			{
				int availableWidth  = getWidth() - getPaddingLeft() - getPaddingRight();
				int availableHeight = getHeight() - getPaddingTop() - getPaddingBottom();

				int sideLength = Math.min(availableWidth, availableHeight);

				float left = getPaddingLeft() + (availableWidth - sideLength) / 2f;
				float top = getPaddingTop() + (availableHeight - sideLength) / 2f;

				return new RectF(left, top, left + sideLength, top + sideLength);
			}

		private void updateShaderMatrix ( )
			{
				if (mBitmap == null)
					{
						return;
					}

				float scale;
				float dx = 0;
				float dy = 0;

				mShaderMatrix.set(null);

				int bitmapHeight = mBitmap.getHeight();
				int bitmapWidth = mBitmap.getWidth();

				if (bitmapWidth * mDrawableRect.height() > mDrawableRect.width() * bitmapHeight)
					{
						scale = mDrawableRect.height() / (float) bitmapHeight;
						dx = (mDrawableRect.width() - bitmapWidth * scale) * 0.5f;
					}
				else
					{
						scale = mDrawableRect.width() / (float) bitmapWidth;
						dy = (mDrawableRect.height() - bitmapHeight * scale) * 0.5f;
					}

				mShaderMatrix.setScale(scale, scale);
				mShaderMatrix.postTranslate((int) (dx + 0.5f) + mDrawableRect.left, (int) (dy + 0.5f) + mDrawableRect.top);

				mRebuildShader = true;
			}

		@SuppressLint("ClickableViewAccessibility")
		@Override
		public boolean onTouchEvent ( MotionEvent event )
			{
				if (mDisableCircularTransformation)
					{
						return super.onTouchEvent(event);
					}

				return inTouchableArea(event.getX(), event.getY()) && super.onTouchEvent(event);
			}

		private boolean inTouchableArea ( float x, float y )
			{
				if (mBorderRect.isEmpty())
					{
						return true;
					}

				return Math.pow(x - mBorderRect.centerX(), 2) + Math.pow(y - mBorderRect.centerY(), 2) <= Math.pow(mBorderRadius, 2);
			}

		@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
		private class OutlineProvider extends ViewOutlineProvider
			{

				@Override
				public void getOutline ( View view, Outline outline )
					{
						if (mDisableCircularTransformation)
							{
								ViewOutlineProvider.BACKGROUND.getOutline(view, outline);
							}
						else
							{
								Rect bounds = new Rect();
								mBorderRect.roundOut(bounds);
								outline.setRoundRect(bounds, bounds.width() / 2.0f);
							}
					}

			}

	}
