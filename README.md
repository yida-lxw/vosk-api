# Vosk Speech Recognition Toolkit

Vosk is an offline open source speech recognition toolkit. It enables
speech recognition for 20+ languages and dialects - English, Indian
English, German, French, Spanish, Portuguese, Chinese, Russian, Turkish,
Vietnamese, Italian, Dutch, Catalan, Arabic, Greek, Farsi, Filipino,
Ukrainian, Kazakh, Swedish, Japanese, Esperanto, Hindi, Czech, Polish.
More to come.

Vosk models are small (50 Mb) but provide continuous large vocabulary
transcription, zero-latency response with streaming API, reconfigurable
vocabulary and speaker identification.

Speech recognition bindings implemented for various programming languages
like Python, Java, Node.JS, C#, C++, Rust, Go and others.

Vosk supplies speech recognition for chatbots, smart home appliances,
virtual assistants. It can also create subtitles for movies,
transcription for lectures and interviews.

Vosk scales from small devices like Raspberry Pi or Android smartphone to
big clusters.

# Documentation

For installation instructions, examples and documentation visit [Vosk
Website](https://alphacephei.com/vosk).

# Requirements
Python 3.8+
CentOS 7.x(理论上Ubuntu也可以,但是我没有测试过)

# Build(CPU)
cd vosk-api/python <br/>
python setup.py install

# # Build(GPU)
+ 安装Python 3.8+
~~~shell
conda create --name py311 python=3.11
conda activate py311
# 确保python -V返回的版本号与你安装的版本一致
~~~
+ 安装编译的前置依赖
~~~shell
yum install -y autoconf automake cmake gcc g++ git libtool make nano pkg-config zip wget
~~~
+ 编译kaldi
~~~shell
mkdir /dev
cd /dev
git clone -b vosk --single-branch https://github.com/alphacep/kaldi /dev/kaldi
cd /dev/kaldi/tools
sed -i 's:status=0:exit 0:g' extras/check_dependencies.sh
sed -i 's:--enable-ngram-fsts:--enable-ngram-fsts --disable-bin:g' Makefile
make -j $(nproc) openfst cub
cd /dev/kaldi/tools
extras/install_openblas_clapack.sh
cd /dev/kaldi/src
./configure --mathlib=OPENBLAS_CLAPACK --shared --use-cuda
sed -i 's:-msse -msse2:-msse -msse2:g' kaldi.mk
sed -i 's: -O1 : -O3 :g' kaldi.mk
make -j $(nproc) online2 lm rnnlm cudafeat cudadecoder
~~~
> make -j $(nproc) openfst cub这句命令执行过程中可能会抛出如下异常: <br/>
libtool Version mismatch error. <br/>
解决方法:<br/>
rm -rf aclocal.m4 <br/>
autoreconf -ivf <br/>

+ 编译vosk-api
~~~shell
cd /dev
git clone https://github.com/alphacep/vosk-api /dev/vosk-api
cd /dev/vosk-api/src
KALDI_ROOT=/dev/kaldi HAVE_CUDA=1 make -j $(nproc)
python -m pip install --upgrade pip setuptools wheel cython
cd ../python
python ./setup.py bdist_wheel
cd ./dist && pip install vosk-*.whl
~~~


